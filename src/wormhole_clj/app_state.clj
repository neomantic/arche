(ns wormhole-clj.app-state
  (:require
   [environ.core :refer [env]]))

(defn cache-expiry []
  (if-let [expiry (env :cache-expiry)]
    expiry
    600))

(defn base-uri []
  (if-let [uri (env :base-uri)]
    uri
    (throw (Exception. "Missing base uri environmental variable"))))

(defn app-uri-for [path]
  (format "%s%s" (base-uri) path))