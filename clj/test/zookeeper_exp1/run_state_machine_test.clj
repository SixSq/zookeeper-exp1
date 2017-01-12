(ns zookeeper-exp1.run-state-machine-test
  (:require [zookeeper-exp1.test-util :as tu]
            [zookeeper-exp1.run-state-machine :refer :all]
            [clojure.test :refer :all]
            [zookeeper :as zk]))

(use-fixtures :once tu/setup-embedded-zk)
