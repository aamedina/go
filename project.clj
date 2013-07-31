(defproject socket_go "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.0.1"]
                 [compojure "1.1.5"]
                 [ring "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [incanter "1.5.0-SNAPSHOT"]
                 [org.clojure/data.json "0.2.2"]
                 [com.novemberain/monger "1.5.0"]
                 [org.clojure/math.combinatorics "0.0.4"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [gloss "0.2.2-beta5"]
                 [bigml/sampling "2.1.0"]
                 [hiccup "1.0.3"]
                 [prismatic/dommy "0.1.1"]
                 [org.clojure/data.generators "0.1.2"]
                 [com.keminglabs/c2 "0.2.2"]
                 [com.taoensso/carmine "1.7.0"]
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]
                 [org.clojure/core.logic "0.8.3"]
                 [org.clojure/data.priority-map "0.0.2"]
                 [org.clojure/data.zip "0.1.1"]
                 [org.clojure/core.match "0.2.0-alpha12"]
                 [org.clojure/core.memoize "0.5.3"]
                 [org.clojure/core.cache "0.6.3"]
                 [enlive "1.1.1"]
                 [org.clojure/core.unify "0.5.5"]]
  :source-paths ["src/clj"]
  :main socket-go.core
  :plugins [[lein-cljsbuild "0.3.0"]]
  ;; :hooks [leiningen.cljsbuild]
  :cljsbuild
  {:builds [{:source-paths ["src/cljs"]
             :compiler {:output-to "src/clj/socket_go/public/js/cljs.js"
                        :optimizations :whitespace
                        :pretty-print true}}]}
  :jvm-opts ["-Xmx4g"])
