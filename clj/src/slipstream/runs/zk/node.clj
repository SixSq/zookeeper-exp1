(ns slipstream.runs.zk.node
  (:require [slipstream.runs.zk.util :as rzu]
            [zookeeper :as zk]))

(defn get-node
  "Return comma separated node.index string"
  [client run-id node]
  (let [children (zk/children client (rzu/node-znode-path run-id node))]
    (apply str (interpose "," children))))
