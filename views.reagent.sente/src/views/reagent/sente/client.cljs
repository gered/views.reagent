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
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (flush-send-buffer! sente-chsk-map)
  (client/on-open!))

(defn on-receive!
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (let [[event-id event-data] event]
    (client/on-receive! event-data)))

(defn event-msg-handler
  [sente-chsk-map {:keys [event id client-id] :as ev}]
  (let [[ev-id ev-data] event]
    (cond
      (and (= :chsk/state ev-id)
           (:open? ev-data))
      (on-open! sente-chsk-map ev)

      (= :chsk/recv id)
      (on-receive! sente-chsk-map ev))))

(defn init!
  [sente-chsk-map & [options]]
  (reset! client/send-fn #(send-fn sente-chsk-map %))
  (if (:use-default-sente-router? options)
    (sente/start-chsk-router! (:ch-recv sente-chsk-map) #(event-msg-handler sente-chsk-map %))))
