(ns reagent-data-views.server.core
  (:require
    [clojure.core.async :refer [put!]]
    [clj-browserchannel-messaging.server :as browserchannel]
    [views.core :as vc]
    [views.persistence.core :refer [subscriptions]]
    [views.persistence.memory :refer [new-memory-persistence]]
    [views.router :as vr]
    [views.subscribed-views :refer [subscribed-views persistence]]))

(defonce
  ^{:doc "The Views configuration. Used with functions like vexec!, etc."}
  views-config (atom nil))

(defn- send-deltas [client-id topic body]
  (browserchannel/send client-id topic body))

(defn- no-filters? [templates]
  (not (some
         (fn [[_ {:keys [filter-fn]}]]
           (not (nil? filter-fn)))
         templates)))

(defn init!
  "Initializes configuration for the Views system suitable for use with most applications
   that are only using a single database. You should call this function once during
   application startup after your database connection has been configured. If your
   application requires a more complex Views configuration, you can manually configure
   it yourself and pass in the configuration map directly as the only argument.

   Either way, this function needs to be called as in both cases it initializes
   views.router to enable automatic routing of view subscription/unsubscription
   browserchannel messages.

   * db
   Database connection.

   * persistence
   Optional. An instance of views.persistence.core/IPersistence. If not specified, a
   default views.persistence.memory/ViewsMemoryPersistence instance is used which keeps
   all view subscriptions and data in memory (not suitable for large applications).

   * send-fn
   Optional. A function that takes care of sending view data deltas to clients (e.g. a
   function that sends data to a client via BrowserChannel). The function should accept
   3 arguments: client-id, topic and body. If not specified, a default function that
   sends deltas to the subscribed BrowserChannel client is used.

   * subscriber-key
   Optional. A function applied against incoming BrowserChannel subscription messages
   that is used to get the client-id from the message. If not used, the default is
   :client-id.

   * templates
   Map of views used by this application. Keys are the view names and the values are maps
   containing information about each view. The template map values can contain the
   following keys:

   :fn
   Required. A var of a function (e.g. #'func or (var func)) that returns a HoneySQL
   SELECT query map. This query when run should return the data that this view
   represents. The function can receive any number of arguments which will be passed in
   as-is from the view-sig (everything after the name).

   :post-fn
   Optional. A function that can be used to filter the data returned by the view.
   Receives one argument (the data).

   :filter-fn
   Optional. A function that is run before a view subscription is processed. If false
   is returned, the view subscription is denied. This function receives 2 arguments:
   the raw BrowserChannel subscription request message and the view-sig of the view
   being subscribed to.

   Additionally, the template map you pass in can contain metadata (on the map itself)
   that has a :filter-fn key which works the same as individual view filter-fn's
   described above, except that this one will be global and run before subscriptions
   to any view."
  ([config]
    (vr/init! config browserchannel/incoming-messages-pub)
    (reset! views-config config))
  ([db templates & {:keys [persistence send-fn subscriber-key]}]
    (let [persistence    (or persistence (new-memory-persistence))
          send-fn        (or send-fn send-deltas)
          subscriber-key (or subscriber-key :client-id)]
      (init! (-> {:db                db
                  :subscriber-key-fn subscriber-key
                  :templates         templates
                  :persistence       persistence
                  :send-fn           send-fn
                  :unsafe?           (no-filters? templates)}
                 (vc/config)
                 ; these just keep some useful things around that are handy to have
                 ; for 'simpler' use-cases. vc/config returns a map without these
                 ; present (even though we passed them in).
                 ; this is not necessarily desirable behaviour for certain advanced
                 ; configurations!
                 (assoc :namespace :default-ns)
                 (assoc :subscriber-key-fn subscriber-key))))))

(def
  ^{:doc "Middleware for use with clj-browserchannel-messaging that performs important housekeeping operations."}
  views-middleware
  {:on-close (fn [handler]
               (fn [client-id request reason]
                 ; views.router is notified of session disconnects when messages of this type
                 ; are received on the channel passed to views.router/init!. we simply
                 ; inject a disconnect message on this channel when the browserchannel session
                 ; is closed and all is good
                 (put! browserchannel/incoming-messages
                       (-> {:topic :client-channel
                            :body  :disconnect}
                           (assoc (:subscriber-key-fn @views-config) client-id)))
                 (handler client-id request reason)))})

(defn get-subscribed-views
  "Returns information about the views that are currently subscribed to by clients."
  []
  (let [bsv       (:base-subscribed-views @views-config)
        namespace (or (:namespace @views-config) :default-ns)]
    (subscribed-views bsv namespace)))

(defn get-subscriptions
  "Returns a set of subscriber-keys representing clients subscribed to the views
   identified by the list of view signatures specified."
  [view-sigs]
  (let [bsv         (:base-subscribed-views @views-config)
        persistence (persistence bsv)
        namespace   (or (:namespace @views-config) :default-ns)]
    (subscriptions persistence namespace view-sigs)))