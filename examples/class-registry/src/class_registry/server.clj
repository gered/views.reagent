(ns class-registry.server
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.util.response :refer [response]]
    [net.thegeez.browserchannel.server :refer [wrap-browserchannel]]
    [net.thegeez.browserchannel.immutant-async-adapter :refer [wrap-immutant-async-adapter]]
    [immutant.web :as immutant]
    [clj-pebble.core :as pebble]
    [environ.core :refer [env]]
    [clojure.java.jdbc :as jdbc]
    [views.sql.core :refer [vexec! with-view-transaction]]
    [views.sql.view :refer [view]]
    [reagent-data-views.browserchannel.server :as rdv-browserchannel]))

(def dev? (boolean (env :dev)))

(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//10.0.0.20/class_registry"
         :user        "class_registry"
         :password    "s3cr3t"})



;; View functions.

(defn classes-list
  []
  ["SELECT class_id, code, name
    FROM classes
    ORDER BY code"])

(defn people-list
  [& [type]]
  (if type
    ["SELECT people_id, type, first_name, middle_name, last_name, email
      FROM people
      WHERE type = ?
      ORDER BY last_name, first_name"
     type]
    ["SELECT people_id, type, first_name, middle_name, last_name, email
      FROM people
      ORDER BY type, last_name, first_name"]))

(defn class-registry
  [class-id]
  ["SELECT r.registry_id, p.type, p.first_name, p.middle_name, p.last_name
    FROM registry r
    JOIN people p on p.people_id = r.people_id
    WHERE r.class_id = ?
    ORDER BY p.type, p.last_name, p.first_name"
   class-id])

(defn people-registerable-for-class
  [class-id]
  ["SELECT p.people_id, p.type, p.first_name, p.middle_name, p.last_name, p.email
    FROM people p
    WHERE p.people_id NOT IN (SELECT r.people_id
                              FROM registry r
                              WHERE r.class_id = ?)"
   class-id])



;; Views list.

(def views
  [(view :classes db #'classes-list)
   (view :people db #'people-list)
   (view :class-registry db #'class-registry)
   (view :people-registerable-for-class db #'people-registerable-for-class)])



;; SQL operations triggered by AJAX requests.

(defn add-person!
  [{:keys [type first_name middle_name last_name email] :as person}]
  (vexec! db ["INSERT INTO people (type, first_name, middle_name, last_name, email)
               VALUES (?, ?, ?, ?, ?)"
              type first_name middle_name last_name email])
  (response "ok"))

(defn update-person!
  [{:keys [id first_name middle_name last_name email] :as person}]
  (vexec! db ["UPDATE people SET
               first_name = ?, middle_name = ?, last_name = ?, email = ?
               WHERE people_id = ?"
              first_name middle_name last_name email id])
  (response "ok"))

(defn delete-person!
  [id]
  (vexec! db ["DELETE FROM people WHERE people_id = ?" id])
  (response "ok"))

(defn add-class!
  [{:keys [code name] :as class}]
  (vexec! db ["INSERT INTO classes (code, name) VALUES (?, ?)" code, name])
  (response "ok"))

(defn update-class!
  [{:keys [class_id code name] :as class}]
  (vexec! db ["UPDATE classes SET code = ?, name = ? WHERE class_id = ?" code name class_id])
  (response "ok"))

(defn delete-class!
  [id]
  (vexec! db ["DELETE FROM classes WHERE class_id = ?" id])
  (response "ok"))

(defn add-registration!
  [class-id people-id]
  (vexec! db ["INSERT INTO registry (class_id, people_id) VALUES (?, ?)" class-id people-id])
  (response "ok"))

(defn remove-registration!
  [registry-id]
  (vexec! db ["DELETE FROM registry WHERE registry_id = ?" registry-id])
  (response "ok"))



;; Compojure routes and Ring handler

(def app-routes
  (routes
    ; ajax db action routes
    (POST "/people/add" [person]                  (add-person! person))
    (POST "/people/update" [person]               (update-person! person))
    (POST "/people/delete" [id]                   (delete-person! id))

    (POST "/class/add" [class]                    (add-class! class))
    (POST "/class/update" [class]                 (update-class! class))
    (POST "/class/delete" [id]                    (delete-class! id))

    (POST "/registry/add" [class-id people-id]    (add-registration! class-id people-id))
    (POST "/registry/remove" [id]                 (remove-registration! id))

    ; main page
    (GET "/" [] (pebble/render-resource
                  "html/app.html"
                  {:dev       dev?
                   :csrfToken *anti-forgery-token*}))

    (route/resources "/")
    (route/not-found "not found")))

(def handler
  (-> app-routes
      (wrap-restful-format :formats [:transit-json])
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] (not dev?)))
      (wrap-browserchannel {} {:middleware [rdv-browserchannel/middleware]})
      (wrap-immutant-async-adapter)))



;; Web server startup & main

(defn run-server []
  (pebble/set-options! :cache (not dev?))

  (rdv-browserchannel/init-views! views)

  (immutant/run handler {:port 8080}))

(defn -main [& args]
  (run-server))
