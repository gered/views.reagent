(ns reagent-data-views.server.core
  (:require
    [clojure.tools.logging :as log]
    [views.core :as views]
    [reagent-data-views.utils :refer [relevant-event?]]))

(defn- handle-subscriptions!
  [client-id view-sig context]
  (log/trace client-id "subscribing to" view-sig)
  (views/subscribe! view-sig client-id context))

(defn- handle-unsubscriptions!
  [client-id view-sig context]
  (log/trace client-id "unsubscribing from" view-sig)
  (views/unsubscribe! view-sig client-id context))

(defn- update-context
  [existing-context]
  (if-let [context-fn (get-in @views/view-system [:reagent-data-views :context-fn])]
    (context-fn existing-context)
    existing-context))

(defn on-close!
  [client-id context]
  (log/trace client-id "on-close, unsubscribing from all views")
  (views/unsubscribe-all! client-id))

(defn on-receive!
  [client-id data context]
  (when (relevant-event? data)
    (let [context          (update-context context)
          [event view-sig] data
          ; for safety, since this is otherwise coming in un-altered from clients
          view-sig         (dissoc view-sig :namespace)]
      (condp = event
        :views/subscribe (handle-subscriptions! client-id view-sig context)
        :views/unsubscribe (handle-unsubscriptions! client-id view-sig context)
        (log/warn client-id "unrecognized event" event "-- full received data:" data))
      ; indicating that we handled the received event
      true)))

(defn set-context-fn!
  [f]
  (swap! views/view-system assoc-in [:reagent-data-views :context-fn] f))