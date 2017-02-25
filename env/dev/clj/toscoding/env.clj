(ns toscoding.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [toscoding.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[toscoding started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[toscoding has shut down successfully]=-"))
   :middleware wrap-dev})
