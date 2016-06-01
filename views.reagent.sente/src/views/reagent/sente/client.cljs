(ns views.reagent.sente.client
  (:require
    [views.reagent.client.core :as client]
    [taoensso.sente :as sente]))

(defonce ^:private send-buffer (atom []))

(defn- sente-connected?
  [sente-chsk-map]
  (:open? @(:state sente-chsk-map)))

(defn send-fn
  [sente-chsk-map data]
  (if-not (sente-connected? sente-chsk-map)
    (swap! send-buffer conj data)
    ((:send-fn sente-chsk-map) data)))

(defn- flush-send-buffer!
  [sente-chsk-map]
  (doseq [data @send-buffer]
    (send-fn sente-chsk-map data))
  (reset! send-buffer []))

(defn on-open!
  "should be called when a new Sente connection is established. ev is the event
   map provided by Sente where id = :chsk/state, and :open? = true. make sure
   to call this function on all connection open events, not just the first one."
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (flush-send-buffer! sente-chsk-map)
  (client/on-open!))

(defn on-receive!
  "should be called whenever a new message is received by Sente from the server.
   ev is the event map provided by Sente where id = :chsk/recv. if this function
   returns true it means the received message was for views.reagent and your
   application code does not need to handle the event itself."
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (let [[event-id event-data] event]
    (client/on-receive! event-data)))

(defn default-event-msg-handler
  "very basic Sente event handler that can be used with Sente's start-chsk-router!
   which makes sure on-open! and on-receive! are called when appropriate. if your
   application does not need to do any custom Sente event handling, then you can
   opt to use this event handler."
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (let [[ev-id ev-data] event]
    (cond
      (and (= :chsk/state ev-id)
           (:open? ev-data))
      (on-open! sente-chsk-map ev)

      (= :chsk/recv id)
      (on-receive! sente-chsk-map ev))))

(defn init!
  "performs initial configuration necessary to hook Sente into views.reagent as the
   client/server messaging backend. should be called once on page-load after the
   Sente channel socket has been created (via make-channel-socket!).

   extra available options specific to views.reagent/sente:

   :use-default-sente-router?
   - if set, enables the use of a default Sente event handler (set via Sente's
     start-chsk-router!). if your application does not need to respond to any
     Sente events itself, then you may wish to use this option."
  [sente-chsk-map & [options]]
  (reset! client/send-fn #(send-fn sente-chsk-map %))
  (if (:use-default-sente-router? options)
    (sente/start-chsk-router! (:ch-recv sente-chsk-map) #(default-event-msg-handler sente-chsk-map %))))
