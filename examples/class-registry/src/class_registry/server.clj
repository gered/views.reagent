(ns class-registry.server
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.util.response :refer [response]]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.immutant :refer [sente-web-server-adapter]]
    [immutant.web :as immutant]
    [hiccup.page :refer [html5 include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [clojure.java.jdbc :as jdbc]
    [views.sql.core :refer [vexec! with-view-transaction]]
    [views.sql.view :refer [view]]
    [views.reagent.sente.server :as vr]))

(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//localhost/class_registry"
         :user        "class_registry"
         :password    "s3cr3t"})



;; Sente socket

(defonce sente-socket (atom {}))

;; View system atom

(defonce view-system (atom {}))



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
  (vexec! view-system db
          ["INSERT INTO people (type, first_name, middle_name, last_name, email)
               VALUES (?, ?, ?, ?, ?)"
           type first_name middle_name last_name email])
  (response "ok"))

(defn update-person!
  [{:keys [id first_name middle_name last_name email] :as person}]
  (vexec! view-system db
          ["UPDATE people SET
            first_name = ?, middle_name = ?, last_name = ?, email = ?
            WHERE people_id = ?"
           first_name middle_name last_name email id])
  (response "ok"))

(defn delete-person!
  [id]
  (vexec! view-system db ["DELETE FROM people WHERE people_id = ?" id])
  (response "ok"))

(defn add-class!
  [{:keys [code name] :as class}]
  (vexec! view-system db ["INSERT INTO classes (code, name) VALUES (?, ?)" code, name])
  (response "ok"))

(defn update-class!
  [{:keys [class_id code name] :as class}]
  (vexec! view-system db ["UPDATE classes SET code = ?, name = ? WHERE class_id = ?" code name class_id])
  (response "ok"))

(defn delete-class!
  [id]
  (vexec! view-system db ["DELETE FROM classes WHERE class_id = ?" id])
  (response "ok"))

(defn add-registration!
  [class-id people-id]
  (vexec! view-system db ["INSERT INTO registry (class_id, people_id) VALUES (?, ?)" class-id people-id])
  (response "ok"))

(defn remove-registration!
  [registry-id]
  (vexec! view-system db ["DELETE FROM registry WHERE registry_id = ?" registry-id])
  (response "ok"))



;; main page html

(defn render-page
  []
  (html5
    [:head
     [:title "Class Registry - views.reagent Example"]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:meta {:name "csrf-token" :content *anti-forgery-token*}]
     (include-css
       "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css"
       "app.css")
     (include-js "cljs/app.js")]
    [:body
     [:div#app [:h1 "This will be replaced by the Class Registry app when the ClojureScript is compiled."]]
     (javascript-tag "class_registry.client.run();")]))



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
    (GET "/" [] (render-page))

    (route/resources "/")
    (route/not-found "not found")))


;; Ring middleware to intercept requests to Sente's channel socket routes
;;
;; Because our ring handler below is also using wrap-restful-format, we need to make
;; sure we catch requests intended to Sente before wrap-restful-format has a chance to
;; modify any of the request params our we'll get errors.
;; This is obviously not the only approach to handling this problem, but it is one that
;; I personally find nice, easy and flexible.

(defn wrap-sente
  [handler uri]
  (fn [request]
    (let [uri-match? (.startsWith (str (:uri request)) uri)
          method     (:request-method request)]
      (cond
        (and uri-match? (= :get method))  ((:ajax-get-or-ws-handshake-fn @sente-socket) request)
        (and uri-match? (= :post method)) ((:ajax-post-fn @sente-socket) request)
        :else                             (handler request)))))

(def handler
  (-> app-routes
      (wrap-restful-format :formats [:transit-json])
      (wrap-sente "/chsk")
      (wrap-defaults site-defaults)))



;; Sente event/message handler

(defn sente-event-msg-handler
  [{:keys [event id uid client-id] :as ev}]
  (if (= id :chsk/uidport-close)
    (vr/on-close! view-system ev)
    (when-not (vr/on-receive! view-system ev)
      ; TODO: any code here needed to handle app-specific receive events
      )))



;; Web server startup & main

(defn run-server
  []
  (reset! sente-socket
          (sente/make-channel-socket!
            sente-web-server-adapter
            {:user-id-fn (fn [request] (get-in request [:params :client-id]))}))
  (sente/start-chsk-router! (:ch-recv @sente-socket) sente-event-msg-handler)

  (vr/init! view-system @sente-socket {:views views})

  (immutant/run handler {:port 8080}))

(defn -main
  [& args]
  (run-server))
