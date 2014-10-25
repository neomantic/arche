(ns arche.resources.core
  (:require [arche.http :as http-helper]
            [liberator.representation  :refer [ring-response as-response]]))

(defn not-acceptable-response [liberator-ctx]
  (ring-response
   {:status 406
    :headers (http-helper/header-accept
              (clojure.string/join ","
                                   ((get-in liberator-ctx [:resource :available-media-types]))))
    :body "Unsupported media-type. Supported media type listed in Accept header."}))

(defn construct-error-map [errors error-messages]
  {:errors
   (into {}
         (map (fn [[attribute error-keys]]
                {attribute
                 (apply vector (map #(get error-messages %) error-keys))})
              errors))})
