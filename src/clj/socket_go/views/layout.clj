(ns socket-go.views.layout
  (:use [hiccup.def :only [defhtml]]
        [hiccup.page :only [html5 include-css include-js]]))

(defhtml layout [title & body]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
   [:title title]   
   (include-css "/static/fonts/stylesheet.css"
                "/static/css/animate.css"
                "/static/css/bootstrap.css"
                "/static/css/application.css")
   (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"
               "/static/js/bootstrap.min.js"
               "/static/js/cljs.js")]  
  [:body
   [:header
    [:div.navbar.navbar-static-top
     [:a.navbar-brand {:href "/"} "Socket.Go"]
     [:ul.nav.pull-right
      [:li ]
      [:li {:class "dropdown"}]      
      [:li [:button.btn.dropdown-toggle {:type "button"
                                         :data-toggle "dropdown"} [:a {:href "/"} [:h4 "Login"]]]]]]]
   [:div.container {:id "main"}
    [:div#content body]]
   [:footer
    [:div.container "&copy; 2013 Adrian Medina"]]])

(defn render [& {:keys [title body]}]
  (layout title body))
