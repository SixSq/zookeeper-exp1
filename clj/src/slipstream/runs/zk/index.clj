(ns slipstream.runs.zk.index
  (:require [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]
            [zookeeper :as zk]))

(defn get-
  "Walk the params of a node index (VM) and build a map of the param:value"
  [client run-id node index]
  (let [index-path (rzu/node-index-znode-path run-id node index)
        children (zk/children client index-path)]
    (for [c children]
      (do
        (println (u/get-data client (str index-path "/" c)))
        (assoc nil c (u/get-data client (str index-path "/" c)))))))

(defn register-change
  [client path channel]
  (zk/children client
               path
               :watcher (partial u/on-change client channel)))
