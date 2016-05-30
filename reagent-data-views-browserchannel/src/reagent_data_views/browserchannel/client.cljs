(ns reagent-data-views.browserchannel.client
  (:require
    [net.thegeez.browserchannel.client :as browserchannel]
    [reagent-data-views.client.core :as client]))

(defn init!
  "performs initial configuration necessary to hook browserchannel into reagent-data-views
   as the client/server messaging backend. should be called once on page load before
   browserchannel is initialized."
  []
  (reset! client/send-fn
          (fn [data]
            (browserchannel/send-data! data))))

(def middleware
  "clj-browserchannel client-side event middleware. this should be included in the
   middleware list provided to net.thegeez.browserchannel.client/connect!"
  {:on-receive
   (fn [handler]
     (fn [data]
       (if-not (client/on-receive! data)
         ; only pass along receive events for data not intended for the views system
         (handler data))))

   :on-opening
   (fn [handler]
     (fn []
       ; we do this in on-opening instead of on-open since with browserchannel we
       ; have the ability to queue up messages to be sent to the server in the initial
       ; connection request. if this connection is actually a reconnection, then any
       ; subscription requests that need to be resent get sent all in one go this way.
       (client/on-open!)
       (handler)))})
