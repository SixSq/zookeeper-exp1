(ns slipstream.runs.zk.run-test
  (:require [slipstream.zk.util-test :as ut]
            [slipstream.runs.zk.run :refer :all]
            [slipstream.runs.zk.run-state-machine :as rsm]
            [slipstream.runs.zk.state :as s]
            [slipstream.zk.util :as u]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.zk.util-test :as rzut]
            [slipstream.buddy-circle.core :as bc]
            [clojure.test :refer :all]
            [zookeeper :as zk]
            [clojure.string :as str]))

(use-fixtures :once ut/setup-embedded-zk)

(deftest create-test
  (testing "create run"
    (let [run-id (create @ut/client rzut/module-a {})]
      (let [leaf-nodes (u/walk @ut/client (rzu/nodes-znode-path run-id))
            run-path (rzu/run-znode-path run-id)]
        ;(println (str/join "\n" leaf-nodes))
        (is (= 15 (count leaf-nodes)))
        (is (=  (str run-path "/nodes/node1/1/params/p1") (first leaf-nodes)))
        (is (=  (str run-path "/nodes/node2/2/params/p6") (last leaf-nodes)))))))

(deftest complete-state
  (testing "complete state for each node updates the run state"
    (let [me (bc/join-circle @ut/client "test-run" #())
          run-id (create @ut/client rzut/module-a {})
          init-state rsm/initial-state
          next-state (-> rsm/valid-transitions first val first)]
      (is (= rsm/initial-state (s/get- @ut/client run-id)))
      ;(rzut/print-run run-id)
      ;(println "========")
      (doseq [i (range 1 4)]
        (complete-single-state!
          @ut/client
          me
          run-id
          "node1"
          i
          init-state
          next-state))
      ;(rzut/print-run run-id)
      ;(println "========")
      (doseq [i (range 1 3)]
        (complete-single-state!
          @ut/client
          me
          run-id
          "node2"
          i
          init-state
          next-state))
      (is (= next-state (s/get- @ut/client run-id))))))
