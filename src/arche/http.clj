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

(ns arche.http
  (:require [pandect.core :refer [md5]]
            [clj-time.coerce :refer [to-long to-string]])
  (:import [org.joda.time.format DateTimeFormat]))

(defn- make-header-fn [key]
  (fn [content]
    {key content}))

(def header-location (make-header-fn "Location"))
(def header-accept (make-header-fn "Accept"))
(def header-cache-control (make-header-fn "Cache-Control"))
(def header-etag (make-header-fn "ETag"))

(defn cache-control-header-private-age [number]
  (header-cache-control (format "max-age=%d, private" number)))

(defn digest-make [string]
  (md5 string))

(defn etag-by-body [body]
  (digest-make body))

(defn etag-by-record [table-name record]
  ;; this duplicatios how RR creates etag
  (let [formatter (. DateTimeFormat (forPattern  "YMdHmsS9"))
        stamp-to-s (. formatter (print (to-long (:updated_at record))))]
    (digest-make (format "%s/%d-%s"
                         table-name
                         (:id record)
                         (to-string (:updated_at record))))))
