(ns toscoding.routes.home
  (:require [toscoding.layout :as layout]
            [toscoding.db.core :as db]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))


(def targets (atom nil))

(defn fetch-saved-targets []
  nil) ;; stub to get targets from disk.

(defn fetch-target! [targets]
  (do
    (if (= @targets nil)
      (reset! targets ["http://paul-gowder.com" "http://gowder.io", "http://paultopia.org", "http://standardize.io"]))
    (let [targs @targets
          item (last targs)
          newtargs (butlast targs)] 
      (reset! targets newtargs)
      item)))

(defn enter-data! [s]
  (do
    (db/add-to-db! s)
    (spit "test.txt" (str s "\n\n"))
    (fetch-target! targets)))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/init" []
       (fetch-target! targets))
  (GET "/db" []
       (db/dump-contracts))
  (POST "/file" request (enter-data! (:body-params request)))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
       (response/header "Content-Type" "text/plain; charset=utf-8"))))

