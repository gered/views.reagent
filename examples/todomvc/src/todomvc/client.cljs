(ns todomvc.client
  (:require
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [ajax.core :as ajax]
    [taoensso.sente :as sente]
    [views.reagent.client.component :refer [view-cursor] :refer-macros [defvc]]
    [views.reagent.sente.client :as vr]))

;; Todo MVC - Reagent Implementation
;;
;; This is taken from the example code shown on http://reagent-project.github.io/
;; It has been modified so that instead of using todo data stored client-side in
;; an atom, the data is retrieved from the server.
;;
;; AJAX requests are used to add/edit/delete the todos. The list is refreshed
;; whenever a change is made (by any client currently viewing the app) by a
;; view subscription. See the 'todo-app' component near the bottom-middle of this
;; file for more details about this.



;; Sente socket
;;
;; This just holds the socket map returned by sente's make-channel-socket!
;; so we can refer to it and pass it around as needed.

(defonce sente-socket (atom {}))



;; AJAX operations

(defn add-todo [text]  (ajax/POST "/todos/add" {:format :url :params {:title text}}))
(defn toggle [id]      (ajax/POST "/todos/toggle" {:format :url :params {:id id}}))
(defn save [id title]  (ajax/POST "/todos/update" {:format :url :params {:id id :title title}}))
(defn delete [id]      (ajax/POST "/todos/delete" {:format :url :params {:id id}}))

(defn complete-all [v] (ajax/POST "/todos/mark-all" {:format :url :params {:done? v}}))
(defn clear-done []    (ajax/POST "/todos/delete-all-done"))



;; UI Components

(defn todo-input
  [{:keys [title on-save on-stop]}]
  (let [val (r/atom title)
        stop #(do (reset! val "")
                  (if on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
               (if-not (empty? v) (on-save v))
               (stop))]
    (fn [props]
      [:input (merge (dissoc props :on-save)
                     {:type "text" :value @val :on-blur save
                      :on-change #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                     13 (save)
                                     27 (stop)
                                     nil)})])))

(def todo-edit (with-meta todo-input
                          {:component-did-mount #(.focus (rdom/dom-node %))}))

(defn todo-stats
  [{:keys [filt active done]}]
  (let [props-for (fn [name]
                    {:class (if (= name @filt) "selected")
                     :on-click #(reset! filt name)})]
    [:div
     [:span#todo-count
      [:strong active] " " (case active 1 "item" "items") " left"]
     [:ul#filters
      [:li [:a (props-for :all) "All"]]
      [:li [:a (props-for :active) "Active"]]
      [:li [:a (props-for :done) "Completed"]]]
     (when (pos? done)
       [:button#clear-completed {:on-click clear-done}
        "Clear completed " done])]))

(defn todo-item
  []
  (let [editing (r/atom false)]
    (fn [{:keys [id done title]}]
      [:li {:class (str (if done "completed ")
                        (if @editing "editing"))}
       [:div.view
        [:input.toggle {:type "checkbox" :checked done
                        :on-change #(toggle id)}]
        [:label {:on-double-click #(reset! editing true)} title]
        [:button.destroy {:on-click #(delete id)}]]
       (when @editing
         [todo-edit {:class "edit" :title title
                     :on-save #(save id %)
                     :on-stop #(reset! editing false)}])])))


(defn debug-component
  []
  [:div
   [:div "sente-socket" [:pre (pr-str (:state @sente-socket))]]
   [:div "send-fn" [:pre (pr-str @views.reagent.client.core/send-fn)]]
   [:div "view-data" [:pre (pr-str @views.reagent.client.core/view-data)]]
   [:div [:button {:on-click (fn [e]
                               (println "sending event directly via sente-socket...")
                               ((:send-fn @sente-socket) [:event/foo "foobar!"]))}
          "send direct!"]]
   [:div [:button {:on-click (fn [e]
                               (println "sending event via send-data! ...")
                               (views.reagent.client.core/send-data! [:event/foo "foobar!"]))}
          "send via send-data!"]]
   [:div [:button {:on-click (fn [e]
                               (println (:state @sente-socket)))}
          "current state"]]])


;; Main TODO app component
;;
;; Note that this component is defined using 'defvc' instead of 'defn'. This is a
;; macro provided by views.reagent which is required to be used by any Reagent
;; component that will directly subscribe/unsubscribe to views. It handles all the
;; housekeeping operations that working with views on the client entails.
;;
;; The call to 'view-cursor' is where the rest of the magic happens. This function
;; will:
;;
;; - Send a subscription request to the server for the specified view and parameters
;;   if a subscription for the view (and the exact provided parameters) does not
;;   already exist.
;; - Returns the most recent data for this view in a Reagent cursor. When the data
;;   is changed and the server sends a view refresh, components dereferencing this
;;   cursor will be rerendered, just like any other Reagent atom/cursor.
;; - If the values of the (optional) parameters passed to view-cursor change, a
;;   view resubscription (with the new parameters) will be triggered automatically
;;   and the server will send us new view data.
;;
;; NOTE:
;; view-cursor cannot be used in a Reagent component that was created using defn.

(defvc todo-app
  [props]
  (let [filt (r/atom :all)]
    (fn []
      (let [items  (view-cursor :todos)
            done   (->> @items (filter :done) count)
            active (- (count @items) done)]
        [:div
         [:section#todoapp
          [:header#header
           [:h1 "todos"]
           [todo-input {:id          "new-todo"
                        :placeholder "What needs to be done?"
                        :on-save     add-todo}]]
          (when (-> @items count pos?)
            [:div
             [:section#main
              [:input#toggle-all {:type      "checkbox" :checked (zero? active)
                                  :on-change #(complete-all (pos? active))}]
              [:label {:for "toggle-all"} "Mark all as complete"]
              [:ul#todo-list
               (for [todo (->> @items
                               (filter
                                 (case @filt
                                   :active (complement :done)
                                   :done :done
                                   :all identity))
                               (sort-by :id))]
                 ^{:key (:id todo)} [todo-item todo])]]
             [:footer#footer
              [todo-stats {:active active :done done :filt filt}]]])]
         [:footer#info
          [:p "Double-click to edit a todo"]]]))))



;; Sente event/message handler
;;
;; Note that if you're only using Sente to make use of views.reagent in your app
;; and aren't otherwise using it for any other client/server messaging, you can
;; set :use-default-sente-router? to true in the options passed to
;; views.reagent.sente.client/init! (called in run below). Then you will not
;; need to provide a handler like this to start-chsk-router! as one will be
;; provided automatically.

(defn sente-event-msg-handler
  [{:keys [event id client-id] :as ev}]
  (cond
    (vr/chsk-open-event? ev)
    (vr/on-open! @sente-socket ev)

    (= :chsk/recv id)
    (when-not (vr/on-receive! @sente-socket ev)
      ; on-receive! returns true if the event was a views.reagent event and it
      ; handled it.
      ;
      ; you could put your code to handle your app's own events here
      )))



;; Utility functions for dealing with CSRF Token garbage in AJAX requests.

(defn get-csrf-token
  []
  (when-let [csrf-token-element (.querySelector js/document "meta[name=\"csrf-token\"]")]
    (.-content csrf-token-element)))

(defn add-csrf-token-ajax-interceptor!
  [csrf-token]
  (let [interceptor (ajax/to-interceptor
                      {:name    "CSRF Interceptor"
                       :request #(assoc-in % [:headers "X-CSRF-Token"] csrf-token)})]
    (swap! ajax/default-interceptors #(cons interceptor %))))



;; Page load

(defn ^:export run
  []
  (enable-console-print!)

  (let [csrf-token (get-csrf-token)]
    (if csrf-token (add-csrf-token-ajax-interceptor! csrf-token))

    ; Sente setup. create the socket, storing it in an atom and set up a event
    ; handler using sente's own message router functionality.
    (reset! sente-socket (sente/make-channel-socket! "/chsk" csrf-token {}))

    ; set up a handler for sente events
    (sente/start-chsk-router! (:ch-recv @sente-socket) sente-event-msg-handler)

    ; Configure views.reagent for use with Sente.
    (vr/init! @sente-socket {})

    (rdom/render [todo-app] (.getElementById js/document "app"))))
