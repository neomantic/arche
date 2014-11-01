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

(ns step-definitions.hypermedia-steps
  (:use cucumber.runtime.clj
        clojure.test)
  (:require [cheshire.core :only (parse-string) :as json]
            [arche.media :as media]
            [step-definitions.discoverable-resources-steps :refer :all]
            [environ.core :refer [env]])
  (:import [java.net URI URL]))

(def hosted-link-relations
  #{media/link-relation-profile
    media/link-relation-self
    media/link-relation-type})

(defn verify-url [link-relation-type url]
  (when (contains? hosted-link-relations link-relation-type)
    (verify-app-url url media/hal-media-type)))

(defn verify-links [expected-links actual-links]
  (doseq  [expected-link expected-links]
    (let [[link-relation href] expected-link]
      (is (= (URI. (link-href-get link-relation actual-links)) (URI. href)))
      (verify-url link-relation href))))

(Then #"^I should receive the following \"([^\"]*)\" response:$" [media-type expected-response]
      (if (re-find #".+json\z" media-type)
        (is (= (from-json expected-response)
               (from-json (last-response-body))))
        (throw (Exception. "Currently only supports testing json responses."))))

(Then #"^the resource representation should have exactly the following links:$" [table]
      (let [actual-links (get (json/parse-string (last-response-body)) (name media/keyword-links))
            expected-links (table-rows-map table)]
        ;; make sure the same number of links are present
        (is (= (count expected-links) (count actual-links))
            (format "expected links for %s; got %s" expected-links actual-links))
        ;; make sure the same links relations are there
        (is (= (into #{} (keys expected-links)) (into #{} (keys actual-links)))
            "received incorrect link relations")
        ;; check the hrefs
        (verify-links expected-links actual-links)))

(Then #"^the resource representation should have at least the following links:$" [table]
      (let [actual-links (get (json/parse-string (last-response-body)) (name media/keyword-links))
            expected-links (table-rows-map table)]
        (doseq [expected-link expected-links]
          (is (some #(= % (first expected-link)) (keys actual-links))))
        (verify-links expected-links actual-links)))

(Then #"^the resource representation \"([^\"]*)\" property should have the following items:$" [property table]
      (let [parsed (json/parse-string (last-response-body))
            actual (get parsed property)]
        (is (not (nil? actual)) (format "missing the '%s' property; got %s" property parsed))
        (doseq [expect (rest (table-rows-map table))]
            (is (= (get actual (first expect))) (last expect)))))

(Then #"^the resource representation should have an embedded \"([^\"]*)\" property with the following links and properties:$" [property table]
      (defn collect-link-or-properties [link-or-property]
        (let [table-rows (rest (map vec (.raw table)))]
          (map rest (filter #(= (first %) link-or-property) table-rows))))
      (defn convert-to-map [list-of-vector-couples]
        (apply conj (map #(apply hash-map %) list-of-vector-couples)))
      (let [embedded-content (get (json/parse-string (last-response-body)) (name media/keyword-embedded))
            embedded (get embedded-content property)]
        (is (not (nil? embedded-content))  "expected embedded content; found none")
        (is (not (nil? embedded)) "expected embedded items; found none")
        (let [embedded-map (into {} embedded)
              links (get (into {} (filter #(= (first %) (name media/keyword-links)) embedded-map)) (name media/keyword-links))
              properties (convert-to-map (remove #(= (first %) (name media/keyword-links)) embedded-map))
              expected-properties (collect-link-or-properties "property")
              expected-links (collect-link-or-properties "link")]
          (is (= (count links) (count expected-links)) (format  "expected '%s' links; got '%s'" expected-links links))
          (is (= (count properties) (count expected-properties)) (format "expected '%s' properties; got '%s'" expected-properties properties))
          (doseq [[property-name property-value] expected-properties]
            (is (= (get properties property-name) property-value)))
          (doseq [[link-relation-type url] expected-links]
            (is (= (get-in links [link-relation-type (name media/keyword-href)]) url))
            (verify-url link-relation-type url)))))

(Then #"^the resource representation should have a \"([^\"]*)\" link relation with at least the following properties:$" [arg1 arg2]
      (comment  Express the Regexp above with the code you wish you had  )
      (throw (cucumber.runtime.PendingException.)))

(Then #"^the data form for the \"([^\"]*)\" link relation should contain the following:$" [arg1 arg2]
      (comment  Express the Regexp above with the code you wish you had  )
      (throw (cucumber.runtime.PendingException.)))
