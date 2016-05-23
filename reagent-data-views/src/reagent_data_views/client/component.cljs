(ns reagent-data-views.client.component
  (:require
    [clojure.set :refer [difference]]
    [reagent.core :as r]
    [reagent.ratom :refer [RCursor]]
    [reagent.impl.component :refer [reagent-component?]]
    [reagent-data-views.client.core :as rdv]
    [reagent-data-views.client.utils :refer [update-component-state!]]))

(defn unsubscribe-all!
  "Unsubscribes a component from all it's current view subscriptions.
   NOTE: this function is only intended to be used internally by defvc."
  [this]
  (assert (reagent-component? this))
  (let [last-used-view-sigs (:last-used-view-sigs (r/state this))]
    (rdv/unsubscribe! last-used-view-sigs)
    (update-component-state! this #(dissoc % :used-view-sigs :last-used-view-sigs))))

(defn prepare-for-render!
  "Prepares the used-view/last-used-view sigs state for the upcoming
   component render.
   NOTE: this function is only intended to be used internally by defvc."
  [this]
  (assert (reagent-component? this))
  (let [{:keys [used-view-sigs]} (r/state this)]
    (r/set-state this {:used-view-sigs      #{}
                       :last-used-view-sigs (or used-view-sigs #{})})))

(defn update-subscriptions!
  "Updates view subscriptions by checking what view-sigs were passed to
   any view-cursor calls during the most recent render and comparing
   against the view-sigs that were used during the previous render.
   Automatically subscribes to new view-sigs and unsubscribes from old
   ones only as is needed.
   NOTE: this function is only intended to be used internally by defvc."
  [this]
  (assert (reagent-component? this))
  (let [{:keys [used-view-sigs last-used-view-sigs]} (r/state this)]
    (if (not= used-view-sigs last-used-view-sigs)
      (let [sigs-to-unsub (vec (difference last-used-view-sigs used-view-sigs))
            sigs-to-sub   (vec (difference used-view-sigs last-used-view-sigs))]
        (rdv/update-subscriptions! sigs-to-sub sigs-to-unsub)
        (r/set-state this {:used-view-sigs      #{}
                           :last-used-view-sigs used-view-sigs})))))

(defn view-cursor
  "Returns a Reagent cursor that can be used to access the data for a view.
   If the view-sig is not currently subscribed to, the subscription will be
   added automatically by the containing component, but this function will
   return a cursor pointing to nil data until the server sends the initial
   data for the new subscription (at which point a re-render is triggered).

   This function can only be used with the render function of a component
   defined using defvc.

   NOTE: The data returned by this function is intended to be used in a
         read-only manner. Using this cursor to change the data will *not*
         propagate the changes to the server."
  [view-id & parameters]
  (let [view-sig {:view-id    view-id
                  :parameters (or parameters [])}
        this     (r/current-component)]
    (assert (not (nil? this)) "view-cursor can only be used within a defvc component's render function.")
    (update-component-state! this #(update-in % [:used-view-sigs] conj view-sig))
    (rdv/->view-sig-cursor view-sig)))

(defn loading?
  "Returns true if the given view-cursor is still waiting for initial data
   to be received from the server. Once that first set of data is received,
   this function will always return false and cannot be used to e.g. check
   if an updated set of data is waiting to be received."
  [view-cursor]
  (if (instance? RCursor view-cursor)
    (let [[view-sig & _] (.-path view-cursor)
          cursor         (r/cursor rdv/view-data [view-sig])]
      ; if even :refcount is missing, it means that this is the first render
      ; of the component where the view-cursor was created (a subscription
      ; request has not even been sent to the server yet, so :loading? has
      ; not yet been set yet). once the component's first render is finished,
      ; :loading? will be set as we expect.
      (boolean (or (not (:refcount @cursor))
                   (:loading? @cursor))))))