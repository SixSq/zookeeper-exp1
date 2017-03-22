(ns slipstream.zk.util-test
  (:require [slipstream.zk.util :refer :all]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]
            [clojure.test :refer :all]
            [clojure.string :as str]
            [zookeeper :as zk])
  (:import [org.apache.curator.test TestingServer]))

(def client (atom nil))

(defn print-znodes
  [path]
  (println (str/join "\n" (u/walk @client path))))

(defn create-root-znode
  [client]
  (zk/create client rzu/runs-znode-path :persistent? true))

(defn setup-embedded-zk [f]
  (let [server (TestingServer. port)]
    (reset! client (zk/connect (str "127.0.0.1:" 2181)))
    (create-root-znode @client)
    (f)
    (.close server)))

(deftest lazy-contains?-test
  (testing "contains the element"
    (is (true? (lazy-contains? [1 2 3] 1)))
    (is (false? (lazy-contains? [2 3] 1)))
    (is (true? (lazy-contains? [nil 2 3] nil)))
    (is (false? (lazy-contains? nil nil)))))
