(defproject net.gered/views.reagent.sente "0.2.0-SNAPSHOT"
  :description  "Sente client/server messaging adapter for views.reagent."
  :url          "https://github.com/gered/views.reagent"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies []

  :plugins      [[lein-cljsbuild "1.1.8"]]

  :profiles     {:provided
                 {:dependencies
                  [[org.clojure/clojure "1.10.3"]
                   [org.clojure/clojurescript "1.10.773"]
                   [reagent "1.1.0"]
                   [net.gered/views "1.6.0"]
                   [net.gered/views.reagent "0.2.0"]
                   [com.taoensso/sente "1.16.2"]]}}

  :cljsbuild    {:builds
                 {:main
                  {:source-paths ["src"]}}})
