(ns socket-go.routes.api
  (:require [clojure.string :as string]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [monger.collection :as mc]
            [clojure.data.json :as json]
            monger.json)
  (:use compojure.core
        monger.operators
        hiccup.core
        hiccup.page
        ring.middleware.json
        ring.util.response
        [socket-go.views.index :only [index]]))

(defroutes app-routes
  (GET "/" req index)
  (route/files "/static/" {:root "src/clj/socket_go/public"})
  (route/not-found "404 - Resource Not Found"))

(def assemble-routes
  (routes app-routes))


