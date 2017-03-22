(ns slipstream.runs.resources.index
  (:require [slipstream.runs.resources.util :as ru]
            [slipstream.runs.zk.index :as index]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]
            [aleph.http :as http]
            [manifold.stream :as s]
            [clojure.core.async :as a]))

(defn get-handler
  "Get all the params if a given index (VM) or set a watcher for any of the params"
  [client req]
  ;Pull from the request the run-id and param tuples from the params map
  (let [{{:keys [run-id node index]} :params} req
        path (rzu/node-index-znode-path run-id node index)]
    (if-let [socket (try
                      @(http/websocket-connection req)
                      (catch Exception e nil))]
      (let [c (a/chan)]
        (index/register-change client path c)
        (s/connect c socket))
      (ru/response "text/plain" (index/get- client run-id node index)))))
