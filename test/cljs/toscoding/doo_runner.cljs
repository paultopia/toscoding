(ns toscoding.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [toscoding.core-test]))

(doo-tests 'toscoding.core-test)

