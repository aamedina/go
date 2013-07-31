(ns socket-go.core
  (:gen-class)
  (:require [monger.core :as mg]
            [taoensso.carmine :as redis])
  (:use org.httpkit.server
        ring.middleware.json        
        ring.middleware.reload
        [socket-go.routes.api :only [assemble-routes]]
        [compojure.handler :only [site]])
  (:import [com.mongodb MongoOptions ServerAddress]))

(def pool (redis/make-conn-pool))

(def spec-server (redis/make-conn-spec))

(defmacro wredis [& body] `(redis/with-conn pool spec-server ~@body))

(def handler
  (->
   assemble-routes   
   site
   wrap-json-params
   wrap-json-response
   wrap-json-body))

(defonce server (atom nil))

(defn start-server []
  (when-not (nil? @server) (@server))
  (reset! server (run-server (wrap-reload #'handler) {:ip "127.0.0.1"
                                                      :port 8000
                                                      :thread 8})))

(defn -main
  [& args]
  (mg/connect!)
  (mg/set-db! (mg/get-db "socket_go"))  
  (start-server))
