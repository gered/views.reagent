(defproject todomvc "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [compojure "1.2.1"]
                 [ring "1.3.1"]
                 [ring/ring-defaults "0.1.3" :exclusions [javax.servlet/servlet-api]]
                 [net.thegeez/clj-browserchannel-jetty-adapter "0.0.6"]
                 [clj-browserchannel-messaging "0.0.4"]
                 [clj-pebble "0.2.0"]
                 [cljs-ajax "0.3.3"]
                 [reagent "0.5.0-alpha"]
                 [reagent-data-views "0.1.0-SNAPSHOT"]
                 [views "0.5.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.postgresql/postgresql "9.2-1003-jdbc4"]
                 [honeysql "0.4.3"]
                 [environ "1.0.0"]]

  :plugins      [[lein-cljsbuild "1.0.3"]]

  :main         todomvc.server

  :cljsbuild    {:builds
                 {:main
                  {:source-paths ["src"]
                   :compiler
                   {:preamble      ["reagent/react.js"]
                    :output-to     "resources/public/cljs/client.js"
                    :source-map    "resources/public/cljs/client.js.map"
                    :output-dir    "resources/public/cljs/client"
                    :optimizations :none
                    :pretty-print  true}}}}

  :profiles     {:dev     {:env {:dev true}}

                 :uberjar {:env {:dev false}
                           :hooks [leiningen.cljsbuild]
                           :cljsbuild
                           {:jar true
                            :builds
                            {:main
                             {:compiler
                              ^:replace
                              {:output-to     "resources/public/cljs/client.js"
                               :preamble      ["reagent/react.min.js"]
                               :optimizations :advanced
                               :pretty-print  false}}}}}}

  :aliases      {"uberjar" ["do" "clean" ["cljsbuild clean"] "uberjar"]
                 "cljsdev" ["do" ["cljsbuild" "clean"] ["cljsbuild" "once"] ["cljsbuild" "auto"]]})
