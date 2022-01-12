(defproject todomvc "0.1.0-SNAPSHOT"
  :dependencies  [[org.clojure/clojure "1.10.3"]
                  [org.clojure/clojurescript "1.10.773"]
                  [ring "1.9.4"]
                  [ring/ring-defaults "0.3.3" :exclusions [javax.servlet/servlet-api]]
                  [compojure "1.6.2"]
                  [org.immutant/web "2.1.10"]

                  [org.clojure/java.jdbc "0.7.12"]
                  [org.postgresql/postgresql "42.3.1"]
                  [com.taoensso/sente "1.16.2"]
                  [net.gered/views "1.6-SNAPSHOT"]
                  [net.gered/views.sql "0.2-SNAPSHOT"]
                  [net.gered/views.reagent "0.2-SNAPSHOT"]
                  [net.gered/views.reagent.sente "0.2-SNAPSHOT"]

                  [hiccup "1.0.5"]
                  [reagent "1.1.0"]
                  [cljs-ajax "0.8.4"]
                  [cljsjs/react "17.0.2-0"]
                  [cljsjs/react-dom "17.0.2-0"]]

  :plugins       [[lein-cljsbuild "1.1.8"]]

  :main          todomvc.server

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :main :compiler :output-dir]
                                    [:cljsbuild :builds :main :compiler :output-to]]
  :cljsbuild     {:builds {:main
                           {:source-paths ["src"]
                            :compiler     {:main          todomvc.client
                                           :output-to     "resources/public/cljs/app.js"
                                           :output-dir    "resources/public/cljs/target"
                                           :asset-path    "cljs/target"
                                           :source-map    true
                                           :optimizations :none
                                           :pretty-print  true}}}}

  :profiles      {:dev     {}

                  :uberjar {:aot       :all
                            :hooks     [leiningen.cljsbuild]
                            :cljsbuild {:jar    true
                                        :builds {:main
                                                 {:compiler ^:replace {:output-to     "resources/public/cljs/app.js"
                                                                       :optimizations :advanced
                                                                       :pretty-print  false}}}}}}

  :aliases       {"rundemo" ["do" ["clean"] ["cljsbuild" "once"] ["run"]]
                  "uberjar" ["do" ["clean"] ["uberjar"]]}

  )
