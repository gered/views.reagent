(defproject class-registry "0.1.0-SNAPSHOT"
  :dependencies  [[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [ring "1.4.0"]
                  [ring/ring-defaults "0.2.0" :exclusions [javax.servlet/servlet-api]]
                  [ring-middleware-format "0.7.0"]
                  [compojure "1.4.0"]
                  [org.immutant/web "2.1.4"]

                  [org.clojure/java.jdbc "0.6.1"]
                  [org.postgresql/postgresql "9.4.1208.jre7"]
                  [gered/clj-browserchannel "0.3.2"]
                  [gered/clj-browserchannel-immutant-adapter "0.0.3"]
                  [gered/views "1.5-SNAPSHOT"]
                  [gered/views-sql "0.1.0-SNAPSHOT"]
                  [views.reagent "0.2.0-SNAPSHOT"]
                  [views.reagent.browserchannel "0.1.0-SNAPSHOT"]

                  [hiccup "1.0.5"]
                  [reagent "0.6.0-alpha2"]
                  [cljsjs/bootstrap "3.3.6-1"]
                  [cljs-ajax "0.5.4"]

                  [environ "1.0.3"]]

  :plugins       [[lein-cljsbuild "1.1.3"]
                  [lein-environ "1.0.3"]]

  :main          class-registry.server

  :clean-targets ^{:protect false} [:target-path
                                    [:cljsbuild :builds :main :compiler :output-dir]
                                    [:cljsbuild :builds :main :compiler :output-to]]
  :cljsbuild     {:builds {:main
                           {:source-paths ["src"]
                            :compiler     {:main          class-registry.client
                                           :output-to     "resources/public/cljs/app.js"
                                           :output-dir    "resources/public/cljs/target"
                                           :asset-path    "cljs/target"
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
