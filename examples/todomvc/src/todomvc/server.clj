(ns todomvc.server
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET POST]]
    [compojure.route :as route]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [ring.util.response :refer [response]]
    [net.thegeez.browserchannel.server :refer [wrap-browserchannel]]
    [net.thegeez.browserchannel.immutant-async-adapter :refer [wrap-immutant-async-adapter]]
    [immutant.web :as immutant]
    [hiccup.page :refer [html5 include-css include-js]]
    [hiccup.element :refer [javascript-tag]]
    [environ.core :refer [env]]
    [clojure.java.jdbc :as jdbc]
    [views.sql.core :refer [vexec! with-view-transaction]]
    [views.sql.view :refer [view]]
    [reagent-data-views.browserchannel.server :as rdv]))

(def dev? (boolean (env :dev)))

(def db {:classname   "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname     "//localhost/todomvc"
         :user        "todomvc"
         :password    "s3cr3t"})



;; View system atom
;;
;; We just declare it, don't need to fill it with anything. The call below to
;; reagent-data-views.browserchannel.server/init-views! will take care of it.

(defonce view-system (atom {}))



;; View functions.
;;
;; These are functions which accept any number of parameters provided when the view
;; is subscribed to and run whenever a subscriber needs refreshed data for it.
;;
;; A view function's return value requirement depends on what views IView
;; implementation is being used.
;;
;; This example app is using views-sql, so view templates should return a SQL SELECT
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
     [:title "todomvc with reagent"]
     (include-css "todos.css" "todosanim.css")
     (include-js "cljs/app.js")]
    [:body
     (anti-forgery-field)
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

    ; main page
    (GET "/" [] (render-page))

    (route/resources "/")
    (route/not-found "not found")))

(def handler
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] true #_(not dev?)))
      ; NOTE: We are passing in an empty map for the BrowserChannel event handlers only
      ;       because this todo app is not using BrowserChannel for any purpose other
      ;       then to provide client/server messaging for reagent-data-views. If we
      ;       wanted to use it for client/server messaging in our application as well,
      ;       we could pass in any event handlers we want here and it would not intefere
      ;       with reagent-data-views.
      (wrap-browserchannel {} {:middleware [(rdv/->middleware view-system)]})
      (wrap-immutant-async-adapter)))



;; Web server startup & main

(defn run-server
  []
  ; init-views! takes care of initialization of views and reagent-data-views at the same
  ; time. As a result, we do not need to also call views.core/init! anywhere. The
  ; same arguments and options you are able to pass to views.core/init! can also be
  ; passed in here and they will be forwarded along, as this function is intended to be
  ; a drop-in replacement for views.core/init!.
  ; if you need to shutdown the views system (e.g. if you're using something like
  ; Component or Mount), you can just call views.core/shutdown!.
  (rdv/init! view-system {:views views})

  (immutant/run handler {:port 8080}))

(defn -main
  [& args]
  (run-server))
