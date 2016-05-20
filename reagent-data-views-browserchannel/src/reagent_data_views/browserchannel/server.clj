(ns reagent-data-views.browserchannel.server
  (:require
    [clojure.tools.logging :as log]
    [net.thegeez.browserchannel.server :as browserchannel]
    [views.core :as views]
    [reagent-data-views.server.core :as server]))

(defn configure-views!
  "performs browserchannel-specific initialization on the views system that is
   necessary to hook views and reagent-data-views together via browserchannel."
  []
  (views/set-send-fn!
    (fn [client-id [view-sig view-data]]
      (log/trace client-id "refresh view" view-sig)
      (browserchannel/send-data! client-id [:views/refresh view-sig view-data]))))

(def middleware
  "clj-browserchannel server-side event middleware. this should be included in the
   middleware list provided to wrap-browserchannel."
  {:on-receive
   (fn [handler]
     (fn [client-id request data]
       (if-not (server/on-receive! client-id data {:request request})
         ; only pass along receive events for data not intended for the views system
         (handler client-id request data))))

   :on-close
   (fn [handler]
     (fn [client-id request reason]
       (server/on-close! client-id {:request request})
       (handler client-id request reason)))})
