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

(ns arche.core-spec
  (:require [speclj.core :refer :all]
            [arche.resources.discoverable-resource :refer [names discoverable-resource-create]]
            [korma.db :refer :all]
            [arche.test-support :refer :all]
            [arche.config :refer :all]
            [arche.app-state :refer :all :as app]
            [arche.media :refer :all :as media]
            [arche.core :refer :all :as web]
            [ring.mock.request :refer :all :as ring-mock]
            [ring.util.response :refer [get-header] :as ring]
            [clojurewerkz.urly.core :as urly]
            [arche.resources.profiles :refer :all :as profile]))

(defn arche-request [uri & params]
  (let [uri (urly/url-like uri)]
    (web/handler {:request-method :get,
                  :uri (urly/path-of uri)
                  :query-string (urly/query-of uri),
                  :params (first params)})))

(defn successful? [response]
  (= (:status response) 200))

(defn factory-discoverable-resource-create [resource-name]
  (discoverable-resource-create
   {:resource-name  resource-name
    :link-relation-url (format "%s%s" "http://factory/alps/" resource-name)
    :href (format "%s%s" "http://factory/" resource-name)}))

(describe
 "routes"
 (describe
  "for collection of disoverable resources"
  (after (clean-database))
  (it "support a route to get an individual"
      (do
        (factory-discoverable-resource-create "studies")
        (should-be successful? (arche-request (format "%s%s" "/discoverable_resources/" "studies")))))
  (it "supports the /discoverable_resource route with query params"
      (should-be successful? (arche-request "/discoverable_resources/?page=2")))
  (it "supports the /discoverable_resource route with query params"
      (should-be successful? (arche-request "/discoverable_resources?page=2")))
  (it "supports the /discoverable_resource route"
      (should-be successful? (arche-request "/discoverable_resources"))))
 (describe
  "for profiles"
  (it "supports the apls/DiscoverableResources route"
      (should-be successful? (arche-request "/alps/DiscoverableResources")))))

(doseq [mime-type ["application/vnd.hale+json" "application/hal+json" "application/json"]]
    (let [response (web/handler
                    (header (ring-mock/request :get "/")
                            "Accept" mime-type))
          actual-status (:status response)]
      (describe
       (str "entry points routes for mime-type" mime-type)
       (it "is successful"
           (should-be successful? response))
       (it "returns the correct content type header"
           (should= mime-type (ring/get-header response "Content-Type")))
       (it "returns the correct accept header"
           (should=
            "application/hal+json,application/vnd.hale+json,application/json"
            (ring/get-header response "Accept"))))))

(run-specs)
