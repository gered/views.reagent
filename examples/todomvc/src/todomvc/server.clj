(ns todomvc.server
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
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
         :subname     "//localhost/todomvc"
         :user        "todomvc"
         :password    "s3cr3t"})



;; Sente socket
;;
;; This just holds the socket map returned by sente's make-channel-socket!
;; so we can refer to it and pass it around as needed.

(defonce sente-socket (atom {}))



;; View system atom
;;
;; We just declare it, don't need to fill it with anything. The call below to
;; views.reagent.sente.server/init! will take care of initializing it.

(defonce view-system (atom {}))



;; View functions.
;;
;; These are functions which accept any number of parameters provided when the view
;; is subscribed to and run whenever a subscriber needs refreshed data for it.
;;
;; A view function's return value requirement depends on what views IView
;; implementation is being used.
;;
;; This example app is using views.sql, so view templates should return a SQL SELECT
;; query in a clojure.java.jdbc "sqlvec" which is a vector where the first string is
;; the actual SQL query and is followed by any number of parameters to be used in
;; the query.

(defn todos-list
  []
  ["SELECT id, title, done FROM todos ORDER BY title"])



;; Views list.
;;
;; A definition/declaration of the views in the system. Each view is given an id and
;; points to a function that returns the query that will be used to retrieve the view's
;; data. Also other properties can be provided to each view, such as the database connection.
;;
;; The view id and parameters to the view function get used later on to form a
;; "view signature" or "view-sig" when the client subscribes to a view.

(def views
  [(view :todos db #'todos-list)])



;; SQL operations triggered by AJAX requests.
;;
;; These functions are just your ordinary AJAX request handlers that do the various
;; CRUD operations on the example app's data. The only difference is that instead
;; of using clojure.java.jdbc/execute!, we instead use vexec!.
;;
;; vexec! performs the exact same operation as execute!, except that it also
;; analyzes the SQL query being run and dispatches "hints" to the view system which
;; trigger view refrehses for all subscribers of the views that the hints match.

(defn add-todo!
  [title]
  (vexec! view-system db ["INSERT INTO todos (title) VALUES (?)" title])
  (response "ok"))

(defn delete-todo!
  [id]
  (vexec! view-system db ["DELETE FROM todos WHERE id = ?" id])
  (response "ok"))

(defn update-todo!
  [id title]
  (vexec! view-system db ["UPDATE todos SET title = ? WHERE id = ?" title id])
  (response "ok"))

(defn toggle-todo!
  [id]
  ; note that a transaction is obviously not necessary here as we could have used
  ; just a single UPDATE query. however, it is being done this way to demonstrate
  ; using transactions with vexec!.
  (with-view-transaction
    view-system
    [dt db]
    (let [done? (:done (first (jdbc/query dt ["SELECT done FROM todos WHERE id = ?" id])))]
      (vexec! view-system dt ["UPDATE todos SET done = ? WHERE id = ?" (not done?) id]))
    (response "ok")))

(defn mark-all!
  [done?]
  (vexec! view-system db ["UPDATE todos SET done = ?" done?])
  (response "ok"))

(defn delete-all-done!
  []
  (vexec! view-system db ["DELETE FROM todos WHERE done = true"])
  (response "ok"))



;; main page html

(defn render-page
  []
  (html5
    [:head
     [:meta {:name "csrf-token" :content *anti-forgery-token*}]
     [:title "TodoMVC - views.reagent Example"]
     (include-css "todos.css" "todosanim.css")
     (include-js "cljs/app.js")]
    [:body
     [:div#app [:h1 "This will become todomvc when the ClojureScript is compiled"]]
     (javascript-tag "todomvc.client.run();")]))



;; Compojure routes and Ring handler

(def app-routes
  (routes
    ; db action routes
    (POST "/todos/add" [title]        (add-todo! title))
    (POST "/todos/delete" [id]        (delete-todo! (Integer/parseInt id)))
    (POST "/todos/update" [id title]  (update-todo! (Integer/parseInt id) title))
    (POST "/todos/toggle" [id]        (toggle-todo! (Integer/parseInt id)))
    (POST "/todos/mark-all" [done?]   (mark-all! (Boolean/parseBoolean done?)))
    (POST "/todos/delete-all-done" [] (delete-all-done!))

    ; sente routes
    (GET  "/chsk" request ((:ajax-get-or-ws-handshake-fn @sente-socket) request))
    (POST "/chsk" request ((:ajax-post-fn @sente-socket) request))

    ; main page
    (GET "/" [] (render-page))

    (route/resources "/")
    (route/not-found "not found")))

(def handler
  (-> app-routes
      (wrap-defaults site-defaults)))



;; Sente event/message handler
;;
;; Note that if you're only using Sente to make use of views.reagent in your app
;; and aren't otherwise using it for any other client/server messaging, you can
;; set :use-default-sente-router? to true in the options passed to
;; views.reagent.sente.server/init! (called in run-server below). Then you will
;; not need to provide a handler like this to start-chsk-router! as one will be
;; provided automatically.

(defn sente-event-msg-handler
  [{:keys [event id uid client-id] :as ev}]
  (if (= id :chsk/uidport-close)
    (vr/on-close! view-system ev)
    (when-not (vr/on-receive! view-system ev)
      ; on-receive! returns true if the event was a views.reagent event and it
      ; handled it.
      ;
      ; you could put your code to handle your app's own events here
      )))



;; Web server startup & main

(defn run-server
  []
  ; sente setup. create the socket, storing it in an atom and set up a event
  ; handler using sente's own message router functionality.
  ; in this example app we are setting up sente user-id's to just be the
  ; client-id, but you can obviously set this up however you wish.
  (reset! sente-socket
          (sente/make-channel-socket!
            sente-web-server-adapter
            {:user-id-fn (fn [request] (get-in request [:params :client-id]))}))

  ; set up a handler for sente events
  (sente/start-chsk-router! (:ch-recv @sente-socket) sente-event-msg-handler)

  ; views.reagent.sente.server/init! takes care of two things for us:
  ;
  ;  1. initialization of the base views system
  ;  2. customization of views system config for use with sente
  ;
  ; As a result, we do not also need to call views.core/init! anywhere, as
  ; this performs the exact same function and also accepts the same arguments
  ; and options -- see the docs for views.core/init! for more info.
  ;
  ; If you need to shutdown the views system (e.g. if you're using something like
  ; Component or Mount), you can just call views.core/shutdown!.
  (vr/init! view-system @sente-socket {:views views})

  (immutant/run handler {:port 8080}))

(defn -main
  [& args]
  (run-server))
