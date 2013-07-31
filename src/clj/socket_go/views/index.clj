(ns socket-go.views.index
  (:use [socket-go.views.layout :only [render]]
        hiccup.core
        hiccup.form
        [hiccup.page :only (html5)]
        ))

(defn index [& params]
  (render :title "Socket.Go"
          :body
          [:div
           [:script "socket_go.cljs.index.init()"]
           [:div.container {:id "board"}]
           [:div.container {:id "scores"}
            [:h1#black "Black: "
             [:span#black-score 0]]
            [:h1#white "White: "
             [:span#white-score 0]]]
           ]))



