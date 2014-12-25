(ns reagent-data-views.client.component)

(defmacro def-views-component
  [component-name args view-sigs & body]
  `(defn ~component-name ~args
     (let [gen-view-sigs#  (fn ~args ~view-sigs)
           view-sigs-atom# (atom nil)]
       (reagent.core/create-class
         {:component-will-mount
          (fn [this#]
            (reset! view-sigs-atom# (apply gen-view-sigs# (rest (reagent.core/argv this#))))
            (reagent-data-views.client.core/subscribe! (deref view-sigs-atom#)))

          :component-will-unmount
          (fn [this#]
            (reagent-data-views.client.core/unsubscribe! (deref view-sigs-atom#)))

          :component-did-update
          (fn [this# old-argv#]
            (reagent-data-views.client.core/update-view-component-sigs this# gen-view-sigs# view-sigs-atom#))

          :component-function
          (fn ~args
            ~@body)}))))
