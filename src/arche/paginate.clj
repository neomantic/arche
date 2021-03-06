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

(ns arche.paginate)

(def prev-page-key :has-prev)
(def next-page-key :has-next)
(def prev-page-num-key :prev-page)
(def next-page-num-key :next-page)
(def per-page-key :per-page)

(defn- calculate-per-page
  "Returns the value to be used for the number items returned per page. If
  requested is greater than the default, the default is returned"
  [requested default]
  (if (> requested default)
    default
    requested))

;; Both calculate-offset and calculated-limit, returns integers
;; that constitute the 'window' in the collection. The width of the 'window'
;; exceeds the boundaries of the offset and limit to determine if any
;; items preceed the number of items per page (previous items) or if
;; any items proceed the number of items per page (next items)

(defn calculate-offset
  "Given a page number, the requested per page, and the default per-page,
  returns the offset - the position in the collection from which other
  further items should be collected."
  [page per-page default-per-page]
  (if (or (<= per-page 0) (= default-per-page 0) (<= page 1))
    0
    (dec (* (dec page) (calculate-per-page per-page default-per-page)))))

(defn calculate-limit
  "Calculates the the number of items to collect."
  [page requested-per-page default-per-page]
  (cond
   (or (<= requested-per-page 0) (<= page 0))  0
   (and (> requested-per-page 0) (<= default-per-page 0)) 0
   :else (let [requested (if (> requested-per-page default-per-page)
                           default-per-page
                           requested-per-page)]
           (if (= page 1)
             (+ 1 requested)
             (+ 2 requested)))))

(defn window-has-prev?
  "Returns a boolean indicating if items preceed those
  of the paginated collection."
  [window-total page-number]
  (if (<= page-number 1) false
    (> window-total 0)))

(defn window-has-next?
  "Returns boolean indicating if items proceed those
  of the paginated collection."
  [window-total page-number limit]
  (cond
   (or (<= page-number 0) (= limit 0) (< window-total 0)) false
   (and (= limit 1) (<= window-total 1)) false
   (= page-number 1) (>= window-total limit)
   :else (>= window-total limit)))

(defn records
  "Returns the paginated collection of items"
  [items page limit]
  (cond
   (empty? items) items
   (= page 1) (if (< (count items) limit)
                items
                (apply vector (drop-last items)))
   :else
   (let [remainder (drop 1 items)]
     (if (< (count items) limit)
       remainder
       (apply vector (drop-last remainder))))))

(defn- with-page-numbers
  "Assoc's the previous and next page number of the paginated items"
  [paginated requested-page]
  (let [has-next (next-page-key paginated)
        has-prev (prev-page-key paginated)]
    (assoc
        (cond
         (and has-next has-prev) (assoc paginated
                                   next-page-num-key (inc requested-page)
                                   prev-page-num-key (dec requested-page))
         has-next (assoc paginated next-page-num-key (inc requested-page))
         has-prev (assoc paginated prev-page-num-key (dec requested-page))
         :else paginated)
      :page requested-page)))

(defn paginate-fn
  "Given a function that retrieves items and the default items
  to be retrieved, returns a function that accepts no parameters - returns 1st
  page with defaults; one parameter (a page number) - returns that page
  with defaults; or 2 parameters (a page number, and amount per page). The
  return value is a hash-map in the following form: {:records [],
  :has-prev false, :has-next false, :per-page <default>,
  :next-page 0, :prev-page 0}"
  [fetcher-fn default-per-page]
  (fn paginate
    ([] (paginate 1))
    ([page] (paginate page default-per-page))
    ([page per-page]
       ;; algorithm works like this: get 1 more than the maximum count (peek)
       ;; and if amount returns match, then there is a next-page
       (if (or (<= page 0)
               (<= per-page 0)
               (<= default-per-page 0))
         {prev-page-key false
          next-page-key false
          per-page-key 0
          :records []}
         (let [offset (calculate-offset page per-page default-per-page)
               limit (calculate-limit page per-page default-per-page)
               items (fetcher-fn offset limit)
               window-total (count items)
               has-prev (window-has-prev? window-total page)
               has-next (window-has-next? window-total page limit)]
           (with-page-numbers
             {prev-page-key has-prev
              next-page-key has-next
              per-page-key (calculate-per-page per-page default-per-page)
              :records (records items page limit)} page))))))
