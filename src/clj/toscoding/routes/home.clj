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

(defn fetch-saved-targets []
  (parse-string (slurp (io/resource "ph_realurls.json"))))

(defn fetch-target! [targets]
  (do
    (if (= @targets nil)
      (reset! targets (fetch-saved-targets)))
    (let [targs @targets
          item (last targs)
          newtargs (vec (butlast targs))] 
      (reset! targets newtargs)
      (swap! in-process conj item)
      item)))

(defn remove-from-vec [v item]
  (vec
   (remove #{item} v)))

(defn enter-data! [s]
  (do
    (db/add-to-db! s)
    (swap! in-process remove-from-vec (:url s))
    (fetch-target! targets)))

(defn stop-coding [target]
  (do
    (swap! in-process remove-from-vec target)
    (swap! targets conj target)
    (str "removed: " target)))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/init" []
       (fetch-target! targets))
  (POST "/file" request (enter-data! (:body-params request)))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (POST "/quit" request
        (stop-coding (:body-params request)))
  (GET "/dev-db" [] ; just dump db contents
       (db/dump-contracts))
  (GET "/dev-flush" [] ; get rid of db contents
       (db/clear!! "contracts"))
  (GET "/dev-in-process" [] ; dump in-process list
       (str @in-process)))

