(ns reagent-data-views.utils)

(defn relevant-event?
  [data]
  (and (vector? data)
       (keyword? (first data))
       (= (namespace (first data)) "views")))
