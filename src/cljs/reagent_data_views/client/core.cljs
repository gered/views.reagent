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

; return items in b that don't exist in a
(defn- diff [a b]
  (vec
    (reduce
      (fn [item-a item-b]
        (remove #(= % item-b) item-a))
      a b)))

(declare update-subscriptions!)

(defn update-view-component-sigs
  "Not intended to be used outside of the def-view-component macro's
   internal functionality."
  [owner view-sig-gen-fn view-sigs-atom]
  (let [new-args (rest (r/argv owner))
        old-sigs @view-sigs-atom
        new-sigs (apply view-sig-gen-fn new-args)]
    (when (not= old-sigs new-sigs)
      (let [sigs-to-sub   (diff new-sigs old-sigs)
            sigs-to-unsub (diff old-sigs new-sigs)]
        (update-subscriptions! sigs-to-sub sigs-to-unsub)
        (if (not= old-sigs new-sigs)
          (reset! view-sigs-atom new-sigs))))))

(defn get-view-sig-cursor
  "Returns a Reagent cursor that can be used to access the data for this view.
   NOTE: This is intended to be used in a read-only manner. Using this cursor
         to change the data will *not* propagate to the server or any other
         clients currently subscribed to this view."
  [view-sig]
  (r/cursor [view-sig] view-data))

(defn- get-views-by-name [view-name]
  (filter
    (fn [[view-sig _]]
      (= view-name (first view-sig)))
    @view-data))

(defn get-view-cursor
  "Returns a Reagent cursor that can be used to access the data for the view-sig
   with the specified name. If there is currently multiple subscriptions to views
   with the same name (but different arguments), this will throw an error.
   NOTE: This is intended to be used in a read-only manner. Using this cursor
         to change the data will *not* propagate to the server or any other
         clients currently subscribed to this view."
  [view-name]
  (let [view-sig (get-views-by-name view-name)]
    (if (> (count view-sig) 1)
      (throw (str "More then one view signature by the name \"" view-name "\" found."))
      (get-view-sig-cursor (ffirst view-sig)))))

(defn- add-initial-view-data! [view-sig data]
  (let [cursor (get-view-sig-cursor view-sig)]
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
  (let [cursor (get-view-sig-cursor view-sig)]
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
    (remove-view-data! view-sig)
    (browserchannel/send :views.unsubscribe [view-sig])))

(defn subscribe!
  "Subscribes to the specified view(s). Updates to the data on the server will
   be automatically pushed out. Use get-data-cursor to read this data and
   render it in any component(s)."
  [view-sigs]
  (doseq [view-sig view-sigs]
    (add-initial-view-data! view-sig nil)
    (browserchannel/send :views.subscribe [view-sig])))

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
