(defproject reagent-data-views "0.1.0-SNAPSHOT"
  :description "Support for Reagent components that get pushed realtime database updates from the server."
  :url         "https://github.com/gered/reagent-data-views"
  :license     {:name "MIT License"
                :url  "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0-alpha" :scope "provided"]
                 [clj-browserchannel-messaging "0.0.4"]
                 [views "0.5.0"]]

  :source-paths   ["src/clj"]
  :resource-paths ["src/cljs"])
