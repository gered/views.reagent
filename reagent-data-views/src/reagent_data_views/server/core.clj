(ns reagent-data-views.server.core
  (:require
    [clojure.tools.logging :as log]
    [views.core :as views]
    [reagent-data-views.utils :refer [relevant-event?]]))

(defn on-close!
  [client-id]
  (log/trace client-id "on-close, unsubscribing from all views")
  (views/unsubscribe-all! client-id))

(defn handle-subscriptions!
  [client-id view-sig]
  (log/trace client-id "subscribing to" view-sig)
  (let [{:keys [view-id parameters]} view-sig]
    ; TODO: namespace
    (views/subscribe! nil view-id parameters client-id)))

(defn handle-unsubscriptions!
  [client-id view-sig]
  (log/trace client-id "unsubscribing from" view-sig)
  (let [{:keys [view-id parameters]} view-sig]
    ; TODO: namespace
    (views/unsubscribe! nil view-id parameters client-id)))

(defn on-receive!
  [client-id data]
  (when (relevant-event? data)
    (let [[event view-sig] data]
      (condp = event
        :views/subscribe (handle-subscriptions! client-id view-sig)
        :views/unsubscribe (handle-unsubscriptions! client-id view-sig)
        (log/warn client-id "unrecognized event" event "-- full received data:" data))
      ; indicating that we handled the received event
      true)))
