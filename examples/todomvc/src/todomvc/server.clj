(ns todomvc.server
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [net.thegeez.jetty-async-adapter :refer [run-jetty-async]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.util.response :refer [response]]
    [clj-pebble.core :as pebble]
    [clj-browserchannel-messaging.server :as browserchannel]
    [environ.core :refer [env]]
    [honeysql.helpers :refer :all]
    [clojure.java.jdbc :as sql]
    [reagent-data-views.server.core :as rviews :refer [views-config]]
    [views.db.core :refer [vexec! with-view-transaction]]))

(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//localhost/todomvc"
         :user        "todomvc"
         :password    "s3cr3t"})


;; View templates (functions which return HoneySQL SELECT query maps)

(defn ^:refresh-only todos-view []
  (-> (select :id :title :done)
      (from :todos)))

; the keys in this map are the names of the views, which we can refer to in our cljs code
; (the key + args to the view fn combined make up a "view signature" or "view-sig"
;  e.g. [:items] in this case as it has zero arguments.
(def views
  {:todos {:fn #'todos-view}})


;; SQL operations (affecting the views defined above, so we use vexec! instead of jdbc calls)

(defn add-todo! [title]
  (vexec!
    @views-config
    (-> (insert-into :todos)
        (values [{:title title}])))
  (response "added todo"))

(defn delete-todo! [id]
  (vexec!
    @views-config
    (-> (delete-from :todos)
        (where [:= :id id])))
  (response "deleted todo"))

(defn update-todo! [id title]
  (vexec!
    @views-config
    (-> (update :todos)
        (sset {:title title})
        (where [:= :id id])))
  (response "updated todo"))

(defn toggle-todo! [id]
  ; note that we could have written this operation using a single UPDATE query,
  ; but writing it this way also serves to demonstrate:
  ; - using transactions with vexec!
  ; - that the db connection is available under :db in the views config map and can
  ;   be used to run any ordinary query directly with jdbc (of course)
  (with-view-transaction [vt @views-config]
    (let [done? (:done (first (sql/query (:db vt) ["SELECT done FROM todos WHERE id = ?" id])))]
      (vexec!
        vt
        (-> (update :todos)
            (sset {:done (not done?)})
            (where [:= :id id]))))
    (response "toggled todo")))

(defn mark-all! [done?]
  (vexec!
    @views-config
    (-> (update :todos)
        (sset {:done done?})))
  (response "completed all todos"))

(defn delete-all-done! []
  (vexec!
    @views-config
    (-> (delete-from :todos)
        (where [:= :done true])))
  (response "deleted all done todos"))


;; Compojure routes / Jetty server & main

(def app-routes
  (routes
    ; db action routes
    (POST "/todos/add" [title]        (add-todo! title))
    (POST "/todos/delete" [id]        (delete-todo! (Integer/parseInt id)))
    (POST "/todos/update" [id title]  (update-todo! (Integer/parseInt id) title))
    (POST "/todos/toggle" [id]        (toggle-todo! (Integer/parseInt id)))
    (POST "/todos/mark-all" [done?]   (mark-all! (Boolean/parseBoolean done?)))
    (POST "/todos/delete-all-done" [] (delete-all-done!))

    (GET "/" [] (pebble/render-resource "html/app.html" {:dev (env :dev)}))
    (route/resources "/")
    (route/not-found "not found")))

(defn run-server []
  (browserchannel/init!
    :middleware [rviews/views-middleware])
  (rviews/init! db views)

  (-> app-routes
      (browserchannel/wrap-browserchannel)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (run-jetty-async
        {:port         8080
         :auto-reload? true
         :join?        false}))
  (println "Web app is running at http://localhost:8080/"))

(defn -main [& args]
  (run-server))
