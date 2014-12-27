(ns reagent-data-views.client.utils
  (:require
    [reagent.core :as r]
    [reagent.impl.component :as rcomp]
    [reagent.impl.util :refer [reagent-component?]]))

(defn diff
  "Given two vectors a and b, returns a vector that contains only the
   items from a that do not also exist in b."
  [a b]
  (->> b
       (reduce
         (fn [item-a item-b]
           (remove #(= % item-b) item-a))
         a)
       (vec)))

; TODO: relies on internal Reagent functionality. state-atom is not officially
;       part of the public Reagent API yet, but will probably be part of it in
;       the future. this function may need to be updated at that time.
;       https://github.com/reagent-project/reagent/issues/80#issuecomment-67302125
(defn update-component-state!
  "Updates the Reagent component's internal state atom by swap!-ing in the value
   returned by the function f (which receives the current state atom's value)."
  [owner f]
  (assert (reagent-component? owner))
  (swap! (rcomp/state-atom owner) f))