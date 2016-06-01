(ns views.reagent.sente.server
  (:import
    (clojure.lang Atom))
  (:require
    [clojure.tools.logging :as log]
    [views.core :as views]
    [views.reagent.server.core :as server]
    [taoensso.sente :as sente]))

(defn- views-send-fn
  [sente-chsk-map uid [view-sig view-data]]
  (log/trace uid "refresh view" view-sig)
  ((:send-fn sente-chsk-map) uid [:views/refresh [view-sig view-data]]))

(defn on-close!
  "should be called when a client's Sente connection is closed. ev is the event
   map provided by Sente where id = :chsk/uidport-close."
  [^Atom view-system {:keys [event id uid client-id ring-req ?reply-fn] :as ev}]
  (server/on-close! view-system uid ring-req))

(defn on-receive!
  "should be called whenever a Sente event is raised that is *not* a connection
   closed event (:chsk/uidport-close). ev is the event map provided by Sente
   for the event. if this function returns true it means the received message
   was for views.reagent and your application code does not need to handle the
   event itself."
  [^Atom view-system {:keys [event id uid client-id ring-req ?reply-fn] :as ev}]
  (server/on-receive! view-system uid event ring-req))

(defn default-event-msg-handler
  "very basic Sente event handler that can be used with Sente's start-chsk-router!
   which makes sure on-close! and on-receive! are called when appropriate. if your
   application does not need to do any custom Sente event handling, then you can
   opt to use this event handler."
  [^Atom view-system {:keys [event id uid client-id] :as ev}]
  (if (= id :chsk/uidport-close)
    (on-close! view-system ev)
    (on-receive! view-system ev)))

(defn init!
  "initializes the views system and adds Sente-specific configuration to it to
   enable the necessary hooks into views.reagent. this function acts as a direct
   replacement to calling views.core/init!, so are able to initialize both views
   and views.reagent by calling this function. the arguments and return value are
   the same as in views.core/init! so see that function for more information.

   extra available options specific to views.reagent/sente:

   :context-fn
   - a function that accepts an initial context map created by views.reagent and
     allows your application to add any info necessary to the context map passed
     to various view system functions (such as auth-fn, namespace-fn, etc).

   :use-default-sente-router?
   - if set, enables the use of a default Sente event handler (set via Sente's
     start-chsk-router!). if your application does not need to respond to any
     Sente events itself, then you may wish to use this option."
  ([^Atom view-system sente-chsk-map options]
   (let [options (-> options
                     (assoc :send-fn #(views-send-fn sente-chsk-map %1 %2)))]
     (if (:use-default-sente-router? options)
       (sente/start-chsk-router! (:ch-recv sente-chsk-map) #(default-event-msg-handler view-system %)))
     (views/init! view-system options)
     (server/set-context-fn! view-system (:context-fn options))))
  ([sente-chsk-map options]
   (init! (atom {}) sente-chsk-map options)))
