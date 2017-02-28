(ns toscoding.db.core
    (:require [monger.core :as mg]
              [monger.collection :as mc]
              [monger.operators :refer :all]
              [mount.core :refer [defstate]]
              [toscoding.config :refer [env]]))

(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defn add-to-db! [contract]
  (let [oid (:url contract)]
    (mc/insert db "contracts" (merge {:_id oid} contract))))

(defn see-all-contracts []
  (mc/find-maps db "contracts"))

;; just for dev to test on same sites over again.
(defn clear!! [coll]
  (mc/remove db coll))
