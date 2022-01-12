(defproject net.gered/views.reagent "0.2.0-SNAPSHOT"
  :description  "Reagent plugin for the views library, providing real-time component updates to server-side changes to data."
  :url          "https://github.com/gered/views.reagent"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/tools.logging "1.2.4"]]

  :plugins      [[lein-cljsbuild "1.1.8"]]

  :profiles     {:provided
                 {:dependencies
                  [[org.clojure/clojure "1.10.3"]
                   [org.clojure/clojurescript "1.10.773"]
                   [reagent "1.1.0"]
                   [net.gered/views "1.6-SNAPSHOT"]]}}

  :cljsbuild    {:builds
                 {:main
                  {:source-paths ["src"]}}})
