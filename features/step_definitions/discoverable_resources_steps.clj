;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  arche - A hypermedia resource discovery service
;;
;;  https://github.com/neomantic/arche
;;
;;  Copyright:
;;    2014
;;
;;  License:
;;    LGPL: http://www.gnu.org/licenses/lgpl.html
;;    EPL: http://www.eclipse.org/org/documents/epl-v10.php
;;    See the LICENSE file in the project's top-level directory for details.
;;
;;  Authors:
;;    * Chad Albers
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns step-definitions.discoverable-resources-steps
  (:use cucumber.runtime.clj
        clojure.test)
  (:require [arche.media :as media]
            [arche.resources.discoverable-resource :refer :all]
            [arche.core :as web]
            [arche.initialize :refer :all]
            [clojurewerkz.urly.core :as urly]
            [cheshire.core :refer :all :as json]
            [clj-http.client :as client]
            [arche.config :refer [base-uri jdbc-dbspec]]
            [ring.adapter.jetty :as jetty]
            [clojure.java.jdbc :as jdbc]
            [ring.util.response :as ring]
            [environ.core :refer [env]]))

;; support procedures - for some reason, refactoring these out
;; means clojure or cucumber cannot find these producers
(def test-port 57767)

(def last-response (atom nil))

(defn last-response-set! [new-response]
  (reset! last-response new-response))

(defn last-response-property [property]
  (fn [] (get @last-response property)))

(def last-response-body (last-response-property :body))
(def last-response-headers (last-response-property :headers))
(def last-response-status (last-response-property :status))

;; cucumber helpers
;; {"header1" "headers", "value-of-header1" "value_of-header2"}
(defn table-to-map [table]
  (into {} (map vec (.raw table))))

(defn table-rows-map [table]
  (into {} (rest (table-to-map table))))

(defn link-href-get [link-relation-type links]
  (get-in links [link-relation-type (name media/keyword-href)]))


(defn url-to-test [path]
  (let [path-url (urly/url-like path)]
    (-> (urly/url-like "http://localhost")
      (.mutatePort test-port)
      (.mutatePath (urly/path-of path-url))
      (.mutateQuery (urly/query-of path-url))
      .toString)))

(defn unexpected-response-message [url response]
  (format "expected successful response from %s: got %d, with body '%s'"
          url
          (:status response)
          (:body response)))

(defn execute-get-request [path headers]
  (client/get (url-to-test path) {:throw-exceptions false
                                  :headers headers}))

(defn execute-post-request [path headers body]
  (client/post (url-to-test path) {:throw-exceptions false
                                   :headers headers
                                   :body body}))

(defn call-app-url [url accept-type]
  (if (= (urly/host-of url) (urly/host-of base-uri))
    (let [path (urly/path-of (urly/url-like url))]
      (execute-get-request path {"Accept" accept-type}))
    (throw (Exception. (format "That wasn't an app url from the base-uri %s: %s"
                               base-uri
                               url)))))

(defn verify-app-url [url accept-type]
  (let [{status :status :as response} (call-app-url url accept-type)]
    (if (= 406 status)
      (verify-app-url url (ring/get-header response "Accept"))
      (is (= 200 status)
          (unexpected-response-message url response)))))

(def server (atom nil))

(defn server-start []
  (if @server
    (throw (IllegalStateException. "Server already started."))
    (reset! server (jetty/run-jetty web/handler
                                   {:port test-port
                                    :join? false}))))
(defn server-stop []
  (if (nil? @server)
    (throw (IllegalStateException. "Server already stopped."))
    (do
      (.stop @server)
      (reset! server nil))))

(defn truncate-database []
  (jdbc/db-do-commands
   jdbc-dbspec
   (format "TRUNCATE TABLE %s;" (-> names :tableized name))))


(defn to-json [args]
  (cheshire.core/generate-string
    args))

(defn from-json [args]
  (cheshire.core/parse-string
    args true))

(Before []
        (server-start)
        (truncate-database)
        (seed-entry-point))

(After []
       (reset! last-response nil)
       (server-stop)
       (truncate-database))

(Given #"^no discoverable resource is registered$" []
       (truncate-database))

(Given #"^a discoverable resource exists with the following attributes:$" [table]
       (let [table-map (table-to-map table)]
         (discoverable-resource-create
          {:resource-name (get table-map "resource_name")
           :link-relation-url (get table-map "link_relation_url")
           :href (get table-map "href")})))

(When #"^I invoke the uniform interface method GET to \"([^\"]*)\" accepting \"([^\"]*)\"$" [path media-type]
      (last-response-set!
       (execute-get-request path {"Accept" media-type})))

(When #"^I invoke uniform interface method POST to \"([^\"]*)\" with the \"([^\"]*)\" body and accepting \"([^\"]*)\" responses:$" [path content-type accept-type body]
      (let [headers {"Accept" accept-type
                     "Content-Type" content-type}]
        (last-response-set!
         (execute-post-request
          path
          headers
          (try
            (-> body
                from-json
                to-json)
            (catch Exception e
              (prn "That wasn't json")))))))

(Then #"^the resource representation should have exactly the following properties:$" [table]
      (let [actual (into {} (remove (fn [[key item]] (= key media/keyword-links))
                                    (from-json (last-response-body))))
            map-of-table (table-to-map table)
            expected (zipmap
                      (map keyword (keys map-of-table))
                      (vals map-of-table))]
        (is (= (count expected) (count actual)))
        (is (= (into #{} (keys expected)) (into #{} (keys actual))))
        (doall
         (map (fn [pair]
                (let [[key value] pair]
                  (is (= value (key actual)))))
              expected))))

(Then #"^I should get a response with the following errors:$" [table]
      (let [response-map (from-json (last-response-body))]
        (is (not (nil? (get response-map :errors))))
        (doall
         (map (fn [[attribute message]]
                (is (some #(= % message) (get-in response-map [:errors (keyword attribute)]))
                    (format "expected '%s' in attribute '%s'; got; '%s'"
                            message
                            attribute
                            (clojure.string/join ", " (get-in response-map [:errors (keyword attribute)])) )))
              (rest (map vec (.raw table)))))))

(Given #"^(\d+) discoverable resource exists - including the discoverable resources entry point$" [number-of]
       (dotimes [number (dec (Integer. number-of))]
         (let [record-id (inc number)]
           (discoverable-resource-create
            {:resource-name (format "a-resource-name-%d" record-id)
             :link-relation-url (format "http://test.host/alps/resource-profile-%d" record-id)
             :href (format "http://test.host/a-resource-path-%d" record-id)}))))
