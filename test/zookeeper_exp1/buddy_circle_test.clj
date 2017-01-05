(ns zookeeper-exp1.buddy-circle-test
  (:require [zookeeper-exp1.test-util :refer [client setup-embedded-zk]]
            [zookeeper-exp1.buddy-circle :refer :all]
            [clojure.test :refer :all]
            [zookeeper :as zk]))

(use-fixtures :once setup-embedded-zk)

(deftest node-from-root-path-test
  (testing "extract node name from full path"
    (is (= "node" (node-from-root-path (str nodes-path "/node"))))))

(deftest get-nodes-test
  (testing "extract ordered nodes"
    (doall
      (map #(zk/create-all @client (str nodes-path "/" %)
                           :ephemeral? true) ["n-0000000002" "n-0000000001" "n-0000000003"]))
    (is (= ["n-0000000001" "n-0000000002" "n-0000000003"] (get-nodes @client)))))

(deftest predecessor-test
  (testing "predecessor in the sequence, with wrap around for first"
    (is (= "first" (predecessor @client "second" ["first" "second" "third"])))
    (is (= "third" (predecessor @client "first" ["first" "second" "third"])))
    (is (= "second" (predecessor @client "third" ["first" "second" "third"])))))
