(defproject gered/views.reagent.browserchannel "0.1.0-SNAPSHOT"
  :description  "BrowserChannel client/server messaging adapter for views.reagent."
  :url          "https://github.com/gered/views.reagent"
  :license      {:name "MIT License"
                 :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.8.0"]]

  :plugins      [[lein-cljsbuild "1.1.3"]]

  :profiles     {:provided
                 {:dependencies
                  [[org.clojure/clojure "1.8.0"]
                   [org.clojure/clojurescript "1.8.51"]
                   [reagent "0.6.0-alpha"]
                   [gered/views "1.5-SNAPSHOT"]
                   [gered/views.reagent "0.2.0-SNAPSHOT"]
                   [gered/clj-browserchannel "0.3.1"]]}}

  :cljsbuild    {:builds
                 {:main
                  {:source-paths ["src"]}}})
