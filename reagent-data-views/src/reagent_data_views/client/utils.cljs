(ns reagent-data-views.client.utils
  (:require
    [reagent.core :as r]
    [reagent.impl.component :refer [reagent-component?]]))

(defn update-component-state!
  "Updates the Reagent component's internal state atom by swap!-ing in the value
   returned by the function f (which receives the current state atom's value)."
  [owner f]
  (assert (reagent-component? owner))
  (swap! (r/state-atom owner) f))