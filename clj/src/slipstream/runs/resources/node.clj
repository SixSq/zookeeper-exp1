(ns slipstream.runs.resources.node
  (:require [slipstream.runs.zk.node :as n]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]))

(defn get-
  "Return comma separated node.index string"
  [client run-id node]
  (let [children (u/children client (rzu/node-znode-path run-id node))]
    (apply str (interpose "," children))))

(defn scale
  [client run-id node ])
