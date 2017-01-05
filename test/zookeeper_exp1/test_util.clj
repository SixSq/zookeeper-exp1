(ns zookeeper-exp1.test-util
  (:require [zookeeper-exp1.util :refer :all]
            [clojure.test :refer :all]
            [zookeeper-exp1.buddy-circle :refer :all]
            [zookeeper :as zk])
  (:import [org.apache.curator.test TestingServer]))

(def client (atom nil))

(defn create-root-znode
  [client]
  (zk/create client root-znode-path :persistent? true))

(defn setup-embedded-zk [f]
  (let [server (TestingServer. port)]
    (reset! client (connect))
    (create-root-znode @client)
    (f)
    (.close server)))

(deftest lazy-contains?-test
  (testing "contains the element"
    (is (true? (lazy-contains? [1 2 3] 1)))
    (is (false? (lazy-contains? [2 3] 1)))
    (is (true? (lazy-contains? [nil 2 3] nil)))
    (is (false? (lazy-contains? nil nil)))))
