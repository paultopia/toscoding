(ns toscoding.routes.home
  (:require [toscoding.layout :as layout]
            [cheshire.core :refer [parse-string]]
            [toscoding.db.core :as db]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))


(def targets (atom nil))
(def in-process (atom []))

(def stub-targets ["http://paul-gowder.com" "http://gowder.io", "http://paultopia.org", "http://standardize.io"])

(defn fetch-saved-targets []
  (parse-string (slurp (io/resource "ph_realurls.json")))) ;; stub to get targets from disk.

(defn fetch-target! [targets]
  (do
    (if (= @targets nil)
      (reset! targets (fetch-saved-targets)))
    (let [targs @targets
          item (last targs)
          newtargs (butlast targs)] 
      (reset! targets newtargs)
      (swap! in-process conj item)
      item)))

(defn remove-from-vec [v item]
  (vec
   (remove #{item} v)))

(defn enter-data! [s]
  (do
    (db/add-to-db! s)
    (spit "test.txt" (str s "\n\n"))
    (swap! in-process remove-from-vec (:url s))
    (fetch-target! targets)))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/init" []
       (fetch-target! targets))
  (POST "/file" request (enter-data! (:body-params request)))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (GET "/dev-db" [] ; just dump db contents
       (db/dump-contracts))
  (GET "/dev-flush" [] ; get rid of db contents
       (db/clear!! "contracts"))
  (GET "/dev-in-process" [] ; dump in-process list
       (str @in-process)))

