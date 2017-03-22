(ns slipstream.runs.zk.run-state-machine-test
  (:require [slipstream.zk.util-test :as ut]
            [slipstream.runs.zk.run-state-machine :refer :all]
            [slipstream.zk.util :as u]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.zk.util-test :as rzut]
            [clojure.test :refer :all]
            [zookeeper :as zk]
            [clojure.string :as str]))

(use-fixtures :once ut/setup-embedded-zk)

(def module-a
  {:nodes {"node1" {:params {"p1" {:default "default-value-p1" :desc "description for p1"}
                             "p2" {:default "default-value-p2" :desc "description for p2"}
                             "p3" {:desc "description for p3"}}
                    :mapping {"p1" "node2.p4"}
                    :multiplicity 3}
           "node2" {:params {"p4" {:default "default-value-p4" :desc "description for p4"}
                             "p5" {:default "default-value-p5" :desc "description for p5"}
                             "p6" {:desc "description for p6"}}
                    :mapping {"p6" "node1.p1"}
                    :multiplicity 2}}})

(deftest module-to-nodes-test
  (testing "convert module to nodes, or extract nodes from module"
    (let [nodes (module-to-nodes rzut/module-a {})]
      (is (= {"node1" [1 2 3] "nodes2" [1 2]})))))

(deftest create-test
  (testing "create run state machine"
    (let [run-id (u/uuid)]
      (create @ut/client run-id module-a {})
      (is (not-empty (zk/exists @ut/client (rzu/rsm-nodes-path run-id))))
      (let [leaf-nodes (rzu/walk-run @ut/client run-id)
            run-path (rzu/run-znode-path run-id)]
        ;(println (str/join "\n" leaf-nodes))
        (is (= 7 (count leaf-nodes)))
        (is (=  (str run-path "/state") (first leaf-nodes)))
        (is (= (str run-path "/state-machine-nodes/node1/1" (second leaf-nodes))))
        (is (= (str run-path "/topology" (last leaf-nodes))))))))
