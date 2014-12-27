(ns reagent-data-views.client.component)

(defmacro defvc
  [component-name args view-sigs & body]
  `(defn ~component-name ~args
     (let [gen-view-sigs#         (fn ~args ~view-sigs)
           get-current-view-sigs# (fn [this#]
                                    (apply gen-view-sigs# (rest (reagent.core/argv this#))))]
       (reagent.core/create-class
         {:component-will-mount
          (fn [this#]
            (let [current-view-sigs# (get-current-view-sigs# this#)]
              (reagent-data-views.client.component/subscribe! this# current-view-sigs#)))

          :component-will-unmount
          (fn [this#]
            (reagent-data-views.client.component/unsubscribe-all! this#))

          :component-did-update
          (fn [this# old-argv#]
            (let [new-view-sigs# (get-current-view-sigs# this#)]
              (reagent-data-views.client.component/update-subscriptions! this# new-view-sigs#)))

          :component-function
          (fn ~args
            ~@body)}))))
