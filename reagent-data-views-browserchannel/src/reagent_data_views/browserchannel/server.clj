(ns reagent-data-views.browserchannel.server
  (:import
    (clojure.lang Atom))
  (:require
    [clojure.tools.logging :as log]
    [net.thegeez.browserchannel.server :as browserchannel]
    [views.core :as views]
    [reagent-data-views.server.core :as server]))

(defn- views-send-fn
  [client-id [view-sig view-data]]
  (log/trace client-id "refresh view" view-sig)
  (browserchannel/send-data! client-id [:views/refresh [view-sig view-data]]))

(defn init-views!
  "initializes the views system and adds browserchannel-specific configuration
   to it to enable the necessary hooks into reagent-data-views.
   this function acts as a direct replacement to calling views.core/init!, so
   are able to initialize both views and reagent-data-views by calling this
   function. the arguments and return value are the same as in views.core/init!
   so see that function for more information.

   one additional option :context-fn can be specified which is a function
   that accepts an initial context map created by reagent-data-views and
   allows your application to add any information necessary to the context
   passed to various view system functions (such as auth-fn, namespace-fn, etc)."
  ([^Atom view-system options]
   (let [options (-> options
                     (assoc :send-fn views-send-fn))]
     (views/init! view-system options)
     (server/set-context-fn! view-system (:context-fn options))))
  ([options]
    (init-views! (atom {}) options)))

(defn ->middleware
  "returns clj-browserchannel server-side event middleware for injecting
   reagent-data-views handling into the clj-browserchannel client session
   lifecycle handling. simply include the returned middleware map in your
   Ring handler's wrap-browserchannel options."
  [^Atom view-system]
  {:on-receive
   (fn [handler]
     (fn [client-id request data]
       (if-not (server/on-receive! view-system client-id data {:request request})
         ; only pass along receive events for data not intended for the views system
         (handler client-id request data))))

   :on-close
   (fn [handler]
     (fn [client-id request reason]
       (server/on-close! view-system client-id {:request request})
       (handler client-id request reason)))})
