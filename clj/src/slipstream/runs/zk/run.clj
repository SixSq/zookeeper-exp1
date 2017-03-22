(ns slipstream.runs.zk.run
  "Model the run, as a set of znodes:
  /runs/:run-id/
    state-machine-nodes (see dedicated namespace)
    state
    nodes/:node/:index/:param"
  (:require [slipstream.zk.util :as u]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.zk.run-state-machine :as rsm]
            [zookeeper :as zk]
            [zookeeper.data :as zdata]
            [zookeeper.util :as zutil]))

(declare module-a)

(defn create
  "Create run from module and params map
   module: map of {nodes {<node> {:params {<param> [default-value description]}
                                  :mapping {<param> [mapped-param, ]}}
   params: map of {nodes {<node> {multiplicity:value, cloud-offer:value, params}}}"
  [client module params]
  (let [run-id (u/uuid)
        {:keys [nodes]} module]
    (doseq [n nodes]
      (zk/create-all client (rzu/node-znode-path run-id (key n)) :persistent? true)
      (let [{:keys [params multiplicity mapping]} (val n)]
        (doseq [i (range 1 (inc multiplicity))]
          (zk/create-all client (rzu/params-znode-path run-id (key n) i) :persistent? true)
          (doseq [p params]
            (if-let [default (-> p val :default)]
              (zk/create client (rzu/param-znode-path run-id (key n) i (key p))
                         :data (zdata/to-bytes default)
                         :persistent? true)
              (zk/create client (rzu/param-znode-path run-id (key n) i (key p))
                         :persistent? true))))))

    ;Create run state machine
    (rsm/create client run-id module params)
    run-id))

(defn complete-single-state!
  [client me run-id node-name index current-state next-state]
  (rsm/complete-single-state! client me run-id node-name index current-state next-state))

;; Diagnostics / debugging ;;

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
