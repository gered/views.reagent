(ns reagent-data-views.utils
  (:require
    [clojure.string :as string]))

(defn relevant-event?
  [data]
  (and (vector? data)
       (keyword? (first data))
       (string/starts-with? (name (first data)) "views/")))
