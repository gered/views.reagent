(ns reagent-data-views.client.core
  (:require
    [reagent.core :as r]
    [clj-browserchannel-messaging.client :as browserchannel]))

;; IMPORTANT NOTE:
;; We are using Reagent's built-in RCursor instead of the one provided by reagent-cursor
;; because as of reagent 0.5.0 there is added performance improvements to protect
;; against extra rerenders when deref'ing cursors.
;; This is very important for us here as we store all view subscription data in a single
;; Reagent atom and use cursors keyed by view-sig to access the data. With reagent 0.5.0
;; cursors, Component A deref'ing view-sig X will not rerender when Component B
;; deref'ing view-sig Y receives updated data.

(defonce view-data (r/atom {}))

(defn ->view-sig-cursor
  "Creates and returns a Reagent cursor that can be used to access the data
   for the view corresponding with the view-sig.

   Generally, for code in a component's render function, you should use
   reagent-data-views.client.component/view-cursor instead of using this
   function directly. Use of this function instead requires you to manage
   view subscription/unsubscription yourself.

   NOTE: The data returned by this function is intended to be used in a
         read-only manner. Using this cursor to change the data will *not*
         propagate the changes to the server."
  [view-sig]
  (r/cursor [view-sig :data] view-data))

(defn- inc-view-sig-refcount! [view-sig]
  (let [path [view-sig :refcount]]
    (swap! view-data update-in path #(if % (inc %) 1))
    (get-in @view-data path)))

(defn- dec-view-sig-refcount! [view-sig]
  (let [path [view-sig :refcount]]
    (swap! view-data update-in path #(if % (dec %) 0))
    (get-in @view-data path)))

(defn- add-initial-view-data! [view-sig data]
  (let [cursor (->view-sig-cursor view-sig)]
    (reset! cursor data)))

(defn- remove-view-data! [view-sig]
  (swap! view-data dissoc view-sig))

(defn- apply-delete-deltas [existing-data delete-deltas]
  (reduce
    (fn [result row-to-delete]
      (remove #(= % row-to-delete) result))
    existing-data
    delete-deltas))

(defn- apply-insert-deltas [existing-data insert-deltas]
  (concat existing-data insert-deltas))

(defn- apply-deltas! [view-sig deltas]
  (let [cursor (->view-sig-cursor view-sig)]
    (doseq [{:keys [refresh-set insert-deltas delete-deltas]} deltas]
      (if refresh-set         (reset! cursor refresh-set))
      (if (seq delete-deltas) (swap! cursor apply-delete-deltas delete-deltas))
      (if (seq insert-deltas) (swap! cursor apply-insert-deltas insert-deltas)))))

(defn- handle-view-data-init [{:keys [body]}]
  (doseq [[view-sig data] body]
    (add-initial-view-data! view-sig data)))

(defn- handle-view-deltas [{:keys [body]}]
  (doseq [delta-batch body]
    (doseq [[view-sig deltas] delta-batch]
      (apply-deltas! view-sig deltas))))

(defn unsubscribe!
  "Unsubscribes from all of the specified view(s). No further updates from the
   server will be received for these views and the latest data received from
   the server is cleared."
  [view-sigs]
  (doseq [view-sig view-sigs]
    (let [refcount (dec-view-sig-refcount! view-sig)]
      (when (<= refcount 0)
        (remove-view-data! view-sig)
        (browserchannel/send :views.unsubscribe [view-sig])))))

(defn subscribe!
  "Subscribes to the specified view(s). Updates to the data on the server will
   be automatically pushed out. Use a 'view cursor' to read this data and
   render it in any component(s)."
  [view-sigs]
  (doseq [view-sig view-sigs]
    (let [refcount (inc-view-sig-refcount! view-sig)]
      (when (= refcount 1)
        (add-initial-view-data! view-sig nil)
        (browserchannel/send :views.subscribe [view-sig])))))

(defn update-subscriptions!
  "Unsubscribes from old-view-sigs and then subscribes to new-view-sigs. This
   function should be used when one or more arguments to views that are currently
   subscribed to have changed."
  [new-view-sigs old-view-sigs]
  (unsubscribe! old-view-sigs)
  (subscribe! new-view-sigs))

(defn init!
  "Sets up message handling needed to process incoming view subscription deltas.
   Should be called once on page load after BrowserChannel has been initialized."
  []
  (browserchannel/message-handler :views.init handle-view-data-init)
  (browserchannel/message-handler :views.deltas handle-view-deltas))
