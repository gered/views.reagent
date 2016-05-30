(ns reagent-data-views.sente.server
  (:import
    (clojure.lang Atom))
  (:require
    [clojure.tools.logging :as log]
    [views.core :as views]
    [reagent-data-views.server.core :as server]
    [taoensso.sente :as sente]))

(defn- views-send-fn
  [sente-chsk-map uid [view-sig view-data]]
  (log/trace uid "refresh view" view-sig)
  ((:send-fn sente-chsk-map) uid [:views/refresh [view-sig view-data]]))

(defn on-close!
  [^Atom view-system {:keys [event id uid client-id ring-req ?reply-fn] :as ev}]
  (server/on-close! view-system uid ring-req))

(defn on-receive!
  [^Atom view-system {:keys [event id uid client-id ring-req ?reply-fn] :as ev}]
  (server/on-receive! view-system uid event ring-req))

(defn event-msg-handler
  [^Atom view-system {:keys [event id uid client-id] :as ev}]
  (if (= id :chsk/uidport-close)
    (on-close! view-system ev)
    (on-receive! view-system ev)))

(defn init-views!
  ([^Atom view-system sente-chsk-map options]
   (let [options (-> options
                     (assoc :send-fn #(views-send-fn sente-chsk-map %1 %2)))]
     (if (:use-default-sente-router? options)
       (sente/start-chsk-router! (:ch-recv sente-chsk-map) #(event-msg-handler view-system %)))
     (views/init! view-system options)
     (server/set-context-fn! view-system (:context-fn options))))
  ([sente-chsk-map options]
   (init-views! (atom {}) sente-chsk-map options)))
