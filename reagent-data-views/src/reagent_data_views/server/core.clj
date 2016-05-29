(ns reagent-data-views.server.core
  (:import
    (clojure.lang Atom))
  (:require
    [clojure.tools.logging :as log]
    [views.core :as views]
    [reagent-data-views.utils :refer [relevant-event?]]))

(defn- handle-subscriptions!
  [^Atom view-system client-id view-sig context]
  (log/trace client-id "subscribing to" view-sig)
  (views/subscribe! view-system view-sig client-id context))

(defn- handle-unsubscriptions!
  [^Atom view-system client-id view-sig context]
  (log/trace client-id "unsubscribing from" view-sig)
  (views/unsubscribe! view-system view-sig client-id context))

(defn- update-context
  [^Atom view-system existing-context]
  (if-let [context-fn (get-in @view-system [:reagent-data-views :context-fn])]
    (context-fn existing-context)
    existing-context))

(defn on-close!
  [^Atom view-system client-id context]
  (log/trace client-id "on-close, unsubscribing from all views")
  (views/unsubscribe-all! view-system client-id))

(defn on-receive!
  [^Atom view-system client-id data context]
  (when (relevant-event? data)
    (let [context          (update-context view-system context)
          [event view-sig] data
          ; for safety, since this is otherwise coming in un-altered from clients
          view-sig         (dissoc view-sig :namespace)]
      (condp = event
        :views/subscribe (handle-subscriptions! view-system client-id view-sig context)
        :views/unsubscribe (handle-unsubscriptions! view-system client-id view-sig context)
        (log/warn client-id "unrecognized event" event "-- full received data:" data))
      ; indicating that we handled the received event
      true)))

(defn set-context-fn!
  [^Atom view-system f]
  (swap! view-system assoc-in [:reagent-data-views :context-fn] f))