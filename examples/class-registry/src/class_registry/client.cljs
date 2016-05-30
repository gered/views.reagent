(ns class-registry.client
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [ajax.core :refer [POST default-interceptors to-interceptor]]
    [net.thegeez.browserchannel.client :as browserchannel]
    [reagent-data-views.client.component :refer [view-cursor] :refer-macros [defvc]]
    [reagent-data-views.browserchannel.client :as rdv]))

;; Class Registry - Reagent Data Views example app
;;
;; (This example app is (very) loosely based on one of the examples in the Om tutorial).
;;
;; This application is a little bit more complex for an example app, mostly caused by
;; the UI layout (in place editing, etc). Even still, for anyone well versed in Reagent
;; this code shouldn't be difficult to follow.
;;
;; In a real-world application we probably would not want to be quite this lazy when
;; it comes to usage of view-cursor. However, for an example app I think this does
;; serve to demonstrate how you can very quickly build UI's which reactively update to
;; backend database operations and get a lot of "free" UI refreshes, simplifying your
;; code.



;; AJAX actions

(defn add-person! [person]  (POST "/people/add" {:params {:person person}}))
(defn save-person! [person] (POST "/people/update" {:params {:person person}}))
(defn delete-person! [id]   (POST "/people/delete" {:params {:id id}}))
(defn add-class! [class]    (POST "/class/add" {:params {:class class}}))
(defn save-class! [class]   (POST "/class/update" {:params {:class class}}))
(defn delete-class! [id]    (POST "/class/delete" {:params {:id id}}))

(defn add-registration!
  [class-id people-id]
  (POST "/registry/add" {:params {:class-id class-id :people-id people-id}}))

(defn remove-registration!
  [id]
  (POST "/registry/remove" {:params {:id id}}))



;; helper/utility functions

(defn parse-person-name
  "returns a map with a person's first/last and optionally middle name in it
   when given a string of the format 'first-name middle-name last-name' or
   'first-name last-name'. the middle name can also just be an initial."
  [formatted-full-name]
  (let [[first middle last :as parts] (string/split formatted-full-name #"\s+")
        [first last middle] (if (nil? last) [first middle] [first last middle])
        middle (when middle (string/replace middle "." ""))]
    (if (>= (count parts) 2)
      {:first_name  first
       :last_name   last
       :middle_name middle})))

(defn format-editable-name
  "does the reverse of parse-person-name"
  [{:keys [first_name middle_name last_name] :as person}]
  (if middle_name
    (str first_name " " middle_name " " last_name)
    (str first_name " " last_name)))

(defn display-middle-name
  [{:keys [middle_name] :as person}]
  (if (= (count middle_name) 1)
    (str " " middle_name ".")
    (str " " middle_name)))

(defn display-name
  [{:keys [first_name last_name] :as person}]
  (str last_name ", " first_name (display-middle-name person)))

(defn validate-person
  [person]
  (cond
    (or (string/blank? (:first_name person))
        (string/blank? (:last_name person)))
    (js/alert "Invalid format for person's name. Format is:\n\n\"first_name [middle_name] last_name\"")

    (string/blank? (:email person))
    (js/alert "Email address is required.")

    :else person))

(defn validate-class
  [class]
  (cond
    (string/blank? (:code class))
    (js/alert "Class code is required.")

    (string/blank? (:name class))
    (js/alert "Class name is required.")

    :else class))



;; UI helpers

(defn text-edit
  "text editing component which maintains the text value state in a key within a provided atom"
  [& args]
  (let [[attrs value k] (if (= (count args) 2) (cons {} args) args)]
    [:input.form-control
     (merge
       attrs
       {:type      "text"
        :value     (get @value k)
        :on-change #(swap! value assoc k (-> % .-target .-value))})]))

(defn dropdown-list
  "dropdown list / select component which maintains selection state in a provided atom"
  [& args]
  (let [[attrs args] (if (= (count args) 2) args (cons {} args))
        {:keys [value data value-fn label-fn placeholder default-value]} args
        options (map
                  (fn [item]
                    ^{:key (value-fn item)}
                    [:option {:value (value-fn item)} (label-fn item)])
                  data)
        options (if placeholder
                  (cons ^{:key default-value}
                        [:option {:value default-value} placeholder]
                        options)
                  options)]
    [:select.form-control
     (merge
       attrs
       {:value @value
        :on-change #(reset! value (-> % .-target .-value))})
     options]))



;; People UI

(defn person-info
  "row showing a single person and allowing editing/removal of it"
  []
  (let [editing        (r/atom nil)
        end-editing!   #(reset! editing nil)
        start-editing! (fn [{:keys [people_id email] :as person}]
                         (reset! editing
                                 {:id       people_id
                                  :email    email
                                  :name     (format-editable-name person)}))
        save!          (fn []
                         (let [person (-> (:name @editing)
                                          (parse-person-name)
                                          (merge (select-keys @editing [:id :email])))]
                           (when (validate-person person)
                             (save-person! person)
                             (end-editing!))))
        delete!        (fn [{:keys [people_id] :as person}]
                         (delete-person! people_id))]
    (fn [{:keys [email] :as person}]
      (if @editing
        [:div.row.bg-warning
         [:div.col-sm-5 [text-edit editing :name]]
         [:div.col-sm-5 [text-edit editing :email]]
         [:div.col-sm-2.actions
          [:button.btn.btn-sm.btn-success {:on-click save!} [:span.glyphicon.glyphicon-ok]]
          [:button.btn.btn-sm.btn-default {:on-click end-editing!} [:span.glyphicon.glyphicon-remove]]]]
        ; not-editing display
        [:div.row
         [:div.col-sm-5 [:div.value (display-name person)]]
         [:div.col-sm-5 [:div.value email]]
         [:div.col-sm-2.actions
          [:button.btn.btn-sm.btn-default {:on-click #(start-editing! person)} [:span.glyphicon.glyphicon-pencil]]
          [:button.btn.btn-sm.btn-danger {:on-click #(delete! person)} [:span.glyphicon.glyphicon-remove]]]]))))

(defn new-person
  "row showing entry form for adding a new person"
  [type]
  (let [values (r/atom {})
        add!   (fn []
                 (let [person (merge {:type  type
                                      :email (:email @values)}
                                     (parse-person-name (:name @values)))]
                   (when (validate-person person)
                     (add-person! person)
                     (reset! values {}))))]
    [:div.row
     [:div.col-sm-5 [text-edit {:placeholder "Full name"} values :name]]
     [:div.col-sm-5 [text-edit {:placeholder "Email"} values :email]]
     [:div.col-sm-2.actions
      [:button.btn.btn-sm.btn-primary {:on-click add!} [:span.glyphicon.glyphicon-plus]]]]))

(defn people-list
  "sub-container for showing list of people of a certain type and allowing entry
   of new people of that same type"
  [people-type people]
  [:div.container-fluid.list
   (map
     (fn [{:keys [people_id] :as person}]
       ^{:key people_id} [person-info person])
     people)
   [new-person people-type]])

(defvc people
  "main container for people information"
  []
  (let [professors (view-cursor :people "professor")
        students   (view-cursor :people "student")]
    [:div#people.container-fluid
     [:div.panel.panel-default
      [:div.panel-heading [:h3.panel-title "Professors"]]
      [:div.panel-body [people-list "professor" @professors]]]
     [:div.panel.panel-default
      [:div.panel-heading [:h3.panel-title "Students"]]
      [:div.panel-body [people-list "student" @students]]]]))



;; Class Registry UI

(defn registration-info
  "row showing a class registration (a person registered in the class shown in the
   parent component) and allowing removal of it"
  [{:keys [registry_id type] :as registration}]
  [:div.row
   [:div.col-sm-2.value (string/capitalize type)]
   [:div.col-sm-9.value (display-name registration)]
   [:div.col-sm-1.actions
    [:button.btn.btn-sm.btn-danger {:on-click #(remove-registration! registry_id)}
     [:span.glyphicon.glyphicon-remove]]]])

(defvc new-registration
  "row showing entry form for registering someone into a class"
  []
  (let [value (r/atom "")]
    (fn [class-id]
      (let [people (view-cursor :people-registerable-for-class class-id)
            add!   #(if-let [person-id @value]
                     (when-not (= "" person-id)
                       (add-registration! class-id (js/parseInt person-id))
                       (reset! value "")))]
        [:div.row
         [:div.col-sm-11
          [dropdown-list
           {:data          @people
            :value         value
            :value-fn      :people_id
            :label-fn      #(str
                             (if (= "professor" (:type %)) "(Professor) ")
                             (display-name %))
            :placeholder   "(Select a person to add!)"
            :default-value ""}]]
         [:div.col-sm-1.actions
          [:button.btn.btn-sm.btn-primary {:on-click add!} [:span.glyphicon.glyphicon-plus]]]]))))

(defvc class-registry-list
  "sub-container for showing a list of people registered to a class and allowing
   registration of additional people to this class"
  [{:keys [class_id code name] :as class}]
  (let [registry (view-cursor :class-registry class_id)]
    [:div.col-sm-12.panel.panel-default.class-registry
     [:div.panel-body
      [:h4 "Class Registration"]
      (map
        (fn [{:keys [registry_id] :as registration}]
          ^{:key registry_id} [registration-info registration])
        @registry)
      [new-registration class_id]]]))



;; Classes UI

(defn class-info
  "row showing a single class and allowing editing/removal of it. also has a
   toggle to show/hide the people registered in the class"
  []
  (let [editing          (r/atom nil)
        show-registry?   (r/atom false)
        end-editing!     #(reset! editing nil)
        start-editing!   (fn [class]
                           (reset! editing class)
                           (reset! show-registry? false))
        save!            #(let [class @editing]
                           (when (validate-class class)
                             (save-class! class)
                             (end-editing!)))
        delete!          #(delete-class! (:class_id %))
        toggle-registry! #(swap! show-registry? not)]
    (fn [{:keys [code name] :as class}]
      (if @editing
        [:div.row.bg-warning
         [:div.col-sm-2 [text-edit editing :code]]
         [:div.col-sm-7 [text-edit editing :name]]
         [:div.col-sm-3.actions
          [:button.btn.btn-sm.btn-success {:on-click save!} [:span.glyphicon.glyphicon-ok]]
          [:button.btn.btn-sm.btn-default {:on-click end-editing!} [:span.glyphicon.glyphicon-remove]]]]
        ; not-editing display
        [:div
         {:class (str "row" (if @show-registry? " bg-success"))
          :on-double-click toggle-registry!}
         [:div.col-sm-2.value code]
         [:div.col-sm-7.value name]
         [:div.col-sm-3.actions
          [:button.btn.btn-sm.btn-default {:on-click #(start-editing! class)} [:span.glyphicon.glyphicon-pencil]]
          [:button.btn.btn-sm.btn-danger {:on-click #(delete! class)} [:span.glyphicon.glyphicon-remove]]
          [:button
           {:on-click toggle-registry!
            :class    (str "btn btn-sm btn-info" (if @show-registry? " active"))}
           [:span.glyphicon.glyphicon-user]]]
         (if @show-registry?
           [class-registry-list class])]))))

(defn new-class
  "row showing entry form for adding a new class"
  []
  (let [values (r/atom {})
        add!   (fn []
                 (let [class @values]
                   (when (validate-class class)
                     (add-class! class)
                     (reset! values {}))))]
    (fn []
      [:div.row
       [:div.col-sm-2 [text-edit {:placeholder "Code"} values :code]]
       [:div.col-sm-7 [text-edit {:placeholder "Name"} values :name]]
       [:div.col-sm-3.actions
        [:button.btn.btn-sm.btn-primary {:on-click add!} [:span.glyphicon.glyphicon-plus]]]])))

(defn class-list
  "sub-container for showing list of classes and allowing entry of new classes"
  [classes]
  [:div.container-fluid.list
   (map
     (fn [{:keys [class_id] :as class}]
       ^{:key class_id} [class-info class])
     classes)
   [new-class]])

(defvc classes
  "main container for class information"
  []
  (let [classes (view-cursor :classes)]
    [:div#classes.container-fluid
     [:div.panel.panel-default
      [:div.panel-heading [:h3.panel-title "Classes"]]
      [:div.panel-body [class-list @classes]]]]))

(defn class-registry-app
  "main application container"
  []
  [:div.container-fluid
   [:h1#app-title.page-header "Class Registry " [:small "Reagent Data Views Example"]]
   [:div.row
    [:div.col-sm-6 [people]]
    [:div.col-sm-6 [classes]]]])



;; AJAX CSRF stuff

(defn get-anti-forgery-token
  []
  (if-let [hidden-field (.getElementById js/document "__anti-forgery-token")]
    (.-value hidden-field)))

(def csrf-interceptor
  (to-interceptor {:name "CSRF Interceptor"
                   :request #(assoc-in % [:headers "X-CSRF-Token"] (get-anti-forgery-token))}))

(swap! default-interceptors (partial cons csrf-interceptor))



;; Page load

(defn ^:export run
  []
  (rdv/init!)
  (browserchannel/connect! {} {:middleware [rdv/middleware]})

  (r/render-component [class-registry-app] (.getElementById js/document "app")))
