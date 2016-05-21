(ns reagent-data-views.browserchannel.server
  (:require
    [clojure.tools.logging :as log]
    [net.thegeez.browserchannel.server :as browserchannel]
    [views.core :as views]
    [reagent-data-views.server.core :as server]))

(defn- views-send-fn
  [client-id [view-sig view-data]]
  (log/trace client-id "refresh view" view-sig)
  (browserchannel/send-data! client-id [:views/refresh view-sig view-data]))

(defn init-views!
  "initializes the views system and adds browserchannel-specific configuration
   to it to enable the necessary hooks into reagent-data-views.
   you should call this *instead* of views.core/init!. all of the same
   options can be used. see views.core/init! for more information.

   an additional option :context-fn can be specified which is a function
   that accepts an initial context map created by reagent-data-views and
   allows your application to add any information necessary to the context
   passed to various view system functions (such as auth-fn, namespace-fn, etc)."
  [views & [options]]
  (views/init! views views-send-fn options)
  (server/set-context-fn! (:context-fn options)))

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
