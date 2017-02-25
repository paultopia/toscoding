(ns toscoding.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[toscoding started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[toscoding has shut down successfully]=-"))
   :middleware identity})
