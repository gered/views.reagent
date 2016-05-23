(ns todomvc.client
  (:require
    [reagent.core :as r]
    [ajax.core :refer [POST default-interceptors to-interceptor]]
    [dommy.core :refer-macros [sel1]]
    [net.thegeez.browserchannel.client :as browserchannel]
    [reagent-data-views.client.component :refer [view-cursor] :refer-macros [defvc]]
    [reagent-data-views.browserchannel.client :as rdv-browserchannel]))

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



;; AJAX operations

(defn add-todo [text]  (POST "/todos/add" {:format :url :params {:title text}}))
(defn toggle [id]      (POST "/todos/toggle" {:format :url :params {:id id}}))
(defn save [id title]  (POST "/todos/update" {:format :url :params {:id id :title title}}))
(defn delete [id]      (POST "/todos/delete" {:format :url :params {:id id}}))

(defn complete-all [v] (POST "/todos/mark-all" {:format :url :params {:done? v}}))
(defn clear-done []    (POST "/todos/delete-all-done"))



;; UI Components

(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (r/atom title)
        stop #(do (reset! val "")
                  (if on-stop (on-stop)))
        save #(let [v (-> @val str clojure.string/trim)]
               (if-not (empty? v) (on-save v))
               (stop))]
    (fn [props]
      [:input (merge props
                     {:type "text" :value @val :on-blur save
                      :on-change #(reset! val (-> % .-target .-value))
                      :on-key-down #(case (.-which %)
                                     13 (save)
                                     27 (stop)
                                     nil)})])))

(def todo-edit (with-meta todo-input
                          {:component-did-mount #(.focus (r/dom-node %))}))

(defn todo-stats [{:keys [filt active done]}]
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

(defn todo-item []
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



;; Main TODO app component
;;
;; Note that this component is defined using 'defvc' instead of 'defn'. This is a
;; macro provided by reagent-data-views which is required to be used by any Reagent
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

(defvc todo-app [props]
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



;; Some unfortunately necessary set up to ensure we send the CSRF token back with
;; AJAX requests (clj-browserchannel handles this automatically for it's own HTTP
;; requests, so the set up we do is only for our own application code).

(defn get-anti-forgery-token []
  (if-let [tag (sel1 "meta[name='anti-forgery-token']")]
    (.-content tag)))

(def csrf-interceptor
  (to-interceptor {:name "CSRF Interceptor"
                   :request #(assoc-in % [:headers "X-CSRF-Token"] (get-anti-forgery-token))}))

(swap! default-interceptors (partial cons csrf-interceptor))



;; Page load

(defn ^:export run []
  ; Configure reagent-data-views and then BrowserChannel.
  (rdv-browserchannel/configure!)

  ; NOTE: We are passing in an empty map for the BrowserChannel event handlers only
  ;       because this todo app is not using BrowserChannel for any purpose other
  ;       then to provide client/server messaging for reagent-data-views. If we
  ;       wanted to use it for client/server messaging in our application as well,
  ;       we could pass in any event handlers we want here and it would not intefere
  ;       with reagent-data-views.
  (browserchannel/connect! {} {:middleware [rdv-browserchannel/middleware]})

  (r/render-component [todo-app] (.getElementById js/document "app")))
