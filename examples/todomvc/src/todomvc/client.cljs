(ns todomvc.client
  (:require
    [reagent.core :as reagent :refer [atom]]
    [clj-browserchannel-messaging.client :as browserchannel]
    [reagent-data-views.client.core :as rviews]
    [reagent-data-views.client.component :refer [view-cursor] :refer-macros [defvc]]
    [ajax.core :refer [POST]]))

(defn add-todo [text]  (POST "/todos/add" {:format :url :params {:title text}}))
(defn toggle [id]      (POST "/todos/toggle" {:format :url :params {:id id}}))
(defn save [id title]  (POST "/todos/update" {:format :url :params {:id id :title title}}))
(defn delete [id]      (POST "/todos/delete" {:format :url :params {:id id}}))

(defn complete-all [v] (POST "/todos/mark-all" {:format :url :params {:done? v}}))
(defn clear-done []    (POST "/todos/delete-all-done"))

(defn todo-input [{:keys [title on-save on-stop]}]
  (let [val (atom title)
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
                          {:component-did-mount #(.focus (reagent/dom-node %))}))

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
  (let [editing (atom false)]
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

(defvc todo-app [props]
  (let [filt (atom :all)]
    (fn []
      (let [items (view-cursor [:todos])
            done (->> @items (filter :done) count)
            active (- (count @items) done)]
        [:div
         [:section#todoapp
          [:header#header
           [:h1 "todos"]
           [todo-input {:id "new-todo"
                        :placeholder "What needs to be done?"
                        :on-save add-todo}]]
          (when (-> @items count pos?)
            [:div
             [:section#main
              [:input#toggle-all {:type "checkbox" :checked (zero? active)
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

(defn ^:export run []
  (browserchannel/init!
    :on-connect
    (fn []
      (rviews/init!)
      (reagent/render-component [todo-app] (.-body js/document)))))
