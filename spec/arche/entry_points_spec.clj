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

(ns arche.entry-points-spec
  (:use arche.resources.entry-points)
  (:require [speclj.core :refer :all]
            [arche.core :as web]
            [arche.test-support :refer :all]
            [arche.core-spec :refer [arche-request factory-discoverable-resource-create] :as support]
            [arche.resources.discoverable-resource
             :only (discoverable-resource-first discoverable-resources-all)
             :as record]
            [arche.config :refer [base-uri]]
            [ring.mock.request :refer :all :as ring-mock]
            [ring.util.response :only [:get-header] :as ring]
            [clojurewerkz.urly.core :as urly]
            [arche.media :as media]
            [arche.app-state :as app]))

(def expected-profile-url
  (.toString
   (.mutatePath
    (urly/url-like base-uri)
    "/alps/EntryPoints")))

(def expected-type-url
  (format "%s#%s"
          (.mutatePath
           (urly/url-like base-uri)
           "/alps/EntryPoints")
          "entry_points"))

(def expected-self-url
  (.toString (.mutatePath
              (urly/url-like base-uri)
              route)))

(def mock-request
  (header
   (ring-mock/request :get "/")
   "Accept" "application/hal+json"))

(defn make-request []
  (web/handler mock-request))

(describe
 "route"
 (it "returns the correct route"
     (should= "/" route)))

(describe
 "names"
 (it "returns the correct string for titleized"
     (should= "EntryPoints" (:titleized names)))
 (it "returns the correct value for the alps type"
     (should= "entry_points" (:alps-type names)))
 (it "returns the correct value for the keyword"
     :entry-points (:keyword names)))

(let [subject (entry-points-map)
      get-href-value (fn [link-relation]
                       (get-in subject [media/keyword-links link-relation media/keyword-href]))]
  (describe
   "its map for serialization"
   (it "it includes links"
       (should-contain media/keyword-links subject))
   (it "contains a profile link in it's collection of links"
       (should-contain :profile (media/keyword-links subject)))
   (it "contains the correct value for the profiles href"
       (should= expected-profile-url
                (get-href-value media/link-relation-profile)))
   (it "contains a profile link in it's collection of links"
       (should-contain :type (media/keyword-links subject)))
   (it "contains the correct value for the type href"
       (should= expected-type-url
                (get-href-value  media/link-relation-type)))
   (it "contains a self link in it's collection of links"
       (should-contain :self (media/keyword-links subject)))
   (it "contains the correct value for the self href"
       (should= expected-self-url
                (get-href-value media/link-relation-self)))))

(describe
 "urls"
 (it "returns the correct profile url"
     (should= expected-profile-url (profile-url)))
 (it "returns the correct type url"
     (should= expected-type-url (type-url))))

(let [default-link-relations #{media/link-relation-self
                               media/link-relation-type
                               media/link-relation-profile}
      link-relations (fn [map]
                       (media/keyword-links map))
      test-resource-name "studies"]
  (context
   "with nothing to discover"
   (before
    (truncate-database))
   (describe
    "empty entry-points map"
    (it "has correct default links and nothing more"
        (should== default-link-relations
                  (into #{} (keys (link-relations (entry-points-map))))))))
  (context
   "with something to discover"
   (describe
    "entry-points map"
    (before
     (support/factory-discoverable-resource-create test-resource-name))
    (it "has more items than the default links"
        (should (>
                 (count (link-relations (entry-points-map)))
                 (count default-link-relations))))
    (it "includes a keyword for the discoverable resource"
        (should-contain (keyword test-resource-name)
                        (link-relations (entry-points-map))))

    (it "includes the correct href value for the discoverable resource"
        (let [resource (record/discoverable-resource-first test-resource-name)]
          (if resource
            (should= (:href resource)
                     (media/keyword-href (get (link-relations (entry-points-map)) (keyword test-resource-name))))
            (should-not-be-nil resource)))))))

(describe
   "profile when there are no entry points"
   (before
    (truncate-database))
   (it "creates the correct alps map"
        (should== {:alps {
                          :version "1.0"
                          :doc {
                                :value "Describes the semantics, states and state transitions associated with Entry Points."
                                }
                          :link [
                                 {:href "http://example.org/alps/EntryPoints"
                                  :rel "self"}
                                 ]
                          :descriptor [{
                                        :id "entry_points"
                                        :type "semantic"
                                        :doc  {
                                               :value "A collection of link relations to find resources of a specific type"
                                               }
                                        :descriptor [
                                                     {:href "#list"}
                                                     ]}
                                       {
                                        :id "list"
                                        :name "self"
                                        :type "safe"
                                        :rt "http://example.org/alps/EntryPoints#entry_points"
                                        :doc {:value "Returns a list of entry points"}}]}

                   }
                  (alps-profile-map))))
(let [default-link-relations #{media/link-relation-self
                               media/link-relation-type
                               media/link-relation-profile}
      link-relations (fn [map]
                       (media/keyword-links map))
      test-resource-name "studies"]
  (describe
   "profile where there are entry points"
   (before
    (support/factory-discoverable-resource-create test-resource-name))
   (after
    (truncate-database))
   (it "creates the correct map"
       (should==
        {:alps {
                :version "1.0"
                :doc {
                      :value "Describes the semantics, states and state transitions associated with Entry Points."
                      }
                :link [
                       {:href "http://example.org/alps/EntryPoints"
                        :rel "self"}
                       ]
                :descriptor [{
                              :id "entry_points"
                              :type "semantic"
                              :doc  {
                                     :value "A collection of link relations to find resources of a specific type"
                                     }
                              :descriptor [ {:href "#list"}
                                            {:href (str "#" test-resource-name)}]
                              }
                             {
                              :id "list"
                              :name "self"
                              :type "safe"
                              :rt "http://example.org/alps/EntryPoints#entry_points"
                              :doc {:value "Returns a list of entry points"}
                              }
                             {:id test-resource-name
                              :name test-resource-name
                              :type "safe"
                              :rt (:link_relation_url (record/discoverable-resource-first test-resource-name))
                              :doc {
                                    :value (format
                                            "Returns a resource of the type '%s' as described by its profile"
                                            test-resource-name)
                                    }
                              :link {
                                     :rel "profile"
                                     :href (:link_relation_url (record/discoverable-resource-first test-resource-name))
                                     }}]}

         }
        (alps-profile-map)))))

(describe
 "etags"
 (it "returns an etag"
     (let [response (make-request)]
       (should-not-be-nil (:headers response "Etag"))))
 (it "accepts the etag"
     (let [etag (ring/get-header (make-request) "Etag")
           request (header
                    mock-request
                    "If-None-Match" etag)]
       (should= 304 (:status (web/handler request))))))
