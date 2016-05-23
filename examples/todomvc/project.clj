(defproject todomvc "0.1.0-SNAPSHOT"
  :dependencies  [[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [ring "1.4.0"]
                  [ring/ring-defaults "0.2.0" :exclusions [javax.servlet/servlet-api]]
                  [compojure "1.4.0"]
                  [org.immutant/web "2.1.4"]

                  [org.clojure/java.jdbc "0.6.1"]
                  [org.postgresql/postgresql "9.4.1208.jre7"]
                  [gered/clj-browserchannel "0.3.1"]
                  [gered/clj-browserchannel-immutant-adapter "0.0.3"]
                  [gered/views "1.5-SNAPSHOT"]
                  [gered/views-sql "0.1.0-SNAPSHOT"]
                  [reagent-data-views "0.2.0-SNAPSHOT"]
                  [reagent-data-views-browserchannel "0.1.0-SNAPSHOT"]

                  [clj-pebble "0.2.0"]
                  [reagent "0.6.0-alpha2"]
                  [cljs-ajax "0.5.4"]
                  ; only being used to get a <meta> tag value with the CSRF token in it
                  [prismatic/dommy "1.1.0"]

                  [environ "1.0.3"]]

  :plugins       [[lein-cljsbuild "1.1.3"]
                  [lein-environ "1.0.3"]]

  :main          todomvc.server

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :main :compiler :output-dir]
                                    [:cljsbuild :builds :main :compiler :output-to]]
  :cljsbuild     {:builds {:main
                           {:source-paths ["src"]
                            :compiler     {:output-to     "resources/public/cljs/app.js"
                                           :output-dir    "resources/public/cljs/target"
                                           :source-map    true
                                           :optimizations :none
                                           :pretty-print  true}}}}

  :profiles      {:dev     {:env {:dev "true"}}

                  :uberjar {:env       {}
                            :aot       :all
                            :hooks     [leiningen.cljsbuild]
                            :cljsbuild {:jar    true
                                        :builds {:main
                                                 {:compiler ^:replace {:output-to     "resources/public/cljs/app.js"
                                                                       :optimizations :advanced
                                                                       :pretty-print  false}}}}}}

  :aliases       {"rundemo" ["do" ["clean"] ["cljsbuild" "once"] ["run"]]
                  "uberjar" ["do" ["clean"] ["uberjar"]]}

  )
