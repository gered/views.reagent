(defproject net.gered/views.reagent "0.2.0"
  :description         "Reagent plugin for the views library, providing real-time component updates to server-side changes to data."
  :url                 "https://github.com/gered/views.reagent"
  :license             {:name "MIT License"
                        :url  "http://opensource.org/licenses/MIT"}

  :dependencies        [[org.clojure/tools.logging "1.2.4"]]

  :plugins             [[lein-cljsbuild "1.1.8"]]

  :profiles            {:provided
                        {:dependencies
                         [[org.clojure/clojure "1.10.3"]
                          [org.clojure/clojurescript "1.10.773"]
                          [reagent "1.1.0"]
                          [net.gered/views "1.6.0"]]}}

  :cljsbuild           {:builds
                        {:main
                         {:source-paths ["src"]}}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  ; TODO: uncomment once a future merge of views.reagent and views.reagent.sente is done ...
  ;:release-tasks       [["vcs" "assert-committed"]
  ;                      ["change" "version" "leiningen.release/bump-version" "release"]
  ;                      ["vcs" "commit"]
  ;                      ["vcs" "tag" "v" "--no-sign"]
  ;                      ["deploy"]
  ;                      ["change" "version" "leiningen.release/bump-version"]
  ;                      ["vcs" "commit" "bump to next snapshot version for future development"]
  ;                      ["vcs" "push"]]

  )
