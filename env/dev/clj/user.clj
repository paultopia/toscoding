(ns user
  (:require [mount.core :as mount]
            [toscoding.figwheel :refer [start-fw stop-fw cljs]]
            toscoding.core))

(defn start []
  (mount/start-without #'toscoding.core/http-server
                       #'toscoding.core/repl-server))

(defn stop []
  (mount/stop-except #'toscoding.core/http-server
                     #'toscoding.core/repl-server))

(defn restart []
  (stop)
  (start))


