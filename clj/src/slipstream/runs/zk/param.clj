(ns slipstream.runs.zk.param
  (:require [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]
            [zookeeper :as zk]))

(defn get-
  "Get the value map of the parameter"
  [client run-id node index param]
  (u/get-data client (rzu/param-znode-path run-id node index param)))

(defn register-change
  [client path channel]
  (zk/data client
           path
           :watcher (partial u/on-change client channel)))

(defn set-
  "Set value of the parameter"
  [client run-id node index param value]
  (u/set-data client (rzu/param-znode-path run-id node index param) value))
