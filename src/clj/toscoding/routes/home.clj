(ns toscoding.routes.home
  (:require [toscoding.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defn enter-data [s]
  (do (spit "test.txt" (str s "\n\n"))
      (str "success! posted: " s)))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (POST "/file" request (enter-data (:body-params request)))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
       (response/header "Content-Type" "text/plain; charset=utf-8"))))

