(defproject reagent-data-views "0.2.0-SNAPSHOT"
  :description  "Support for Reagent components that get pushed realtime database updates from the server."
  :url          "https://github.com/gered/reagent-data-views"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/tools.logging "0.3.1"]]

  :profiles     {:provided
                 {:dependencies
                  [[org.clojure/clojure "1.8.0"]
                   [org.clojure/clojurescript "1.8.51"]
                   [reagent "0.6.0-alpha"]
                   [gered/views "1.5-SNAPSHOT"]]}})
