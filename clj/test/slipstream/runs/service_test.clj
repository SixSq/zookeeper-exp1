(ns zookeeper-exp1.service-test
  (:require [zookeeper-exp1.test-util :as tu]
            [clojure.test :refer :all]
            [zookeeper :as zk]
            [zookeeper.util.zk :as util]
            [zookeeper-exp1.service :refer :all]))

(use-fixtures :once tu/setup-embedded-zk)

(deftest a-test
  (testing "FIXME, I succeed."
    (is (= 0 0))))
