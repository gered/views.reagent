(ns reagent-data-views.client.component
  (:require
    [reagent.core :as r]
    [reagent.impl.util :refer [reagent-component?]]
    [reagent-data-views.client.core :as views]
    [reagent-data-views.client.utils :refer [diff update-component-state!]]))

(defn subscribe!
  "Subscribes a component to the given view-sigs.
   NOTE: this function is only intended to be used internally by defvc."
  [this view-sigs]
  (assert (reagent-component? this))
  (r/set-state this {:view-sigs view-sigs})
  (views/subscribe! view-sigs))

(defn unsubscribe-all!
  "Unsubscribes a component from all it's current view subscriptions.
   NOTE: this function is only intended to be used internally by defvc."
  [this]
  (assert (reagent-component? this))
  (when-let [view-sigs (:view-sigs (r/state this))]
    (update-component-state! this #(dissoc % :view-sigs))
    (views/unsubscribe! view-sigs)))

(defn update-subscriptions!
  "Updates a component's view subscriptions to match the new full list
   of view-sigs. Only changed view-sigs will cause a view subscription
   change to occur.
   NOTE: this function is only intended to be used internally by defvc."
  [this new-view-sigs]
  (assert (reagent-component? this))
  (let [current-view-sigs (:view-sigs (r/state this))]
    (when (not= current-view-sigs new-view-sigs)
      (let [sigs-to-sub (diff new-view-sigs current-view-sigs)
            sigs-to-unsub (diff current-view-sigs new-view-sigs)]
        (update-component-state! this #(assoc % :view-sigs new-view-sigs))
        (views/update-subscriptions! sigs-to-sub sigs-to-unsub)))))

(defn- get-views-by-name [view-name view-sigs]
  (filter #(= view-name (first %)) view-sigs))

(defn view-cursor
  "Returns a Reagent cursor that can be used to access the data for a view by
   looking up the corresponding view-sig by name in the current component's
   list of view subscriptions.

   This function can only be used within the component's render function.

   If there are currently multiple subscriptions to views with the same name
   (using different arguments) an error is thrown.

   NOTE: This function is intended to be used in a read-only manner. Using
         this cursor to change the data will *not* propagate to the server or
         any other clients currently subscribed to this view."
  [view-name]
  (assert (not (nil? (r/current-component))))
  (let [view-sigs   (->> (r/current-component) (r/state) :view-sigs)
        match       (get-views-by-name view-name view-sigs)
        num-matches (count match)]
    (case num-matches
      1 (views/view-sig-cursor (first match))
      0 (throw (str "No matching view signature by the name \"" view-name "\"."))
      (throw (str "More then one view signature by the name \"" view-name "\" found.")))))
