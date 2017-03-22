(ns slipstream.runs.zk.state
  (:require [slipstream.runs.zk.param :as p]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]))

(defn get-
  "Extract the :data (as string) of the /runs/<run-id>/state node"
  [client run-id]
  (u/get-data client (rzu/state-znode-path run-id)))

(defn set-
  [client req]
  (let [params (:params req)
        {:keys [run-id node index param]} params
        {:strs [value]} params
        path (rzu/param-znode-path run-id node index param)]
    (u/set-data client path value)))

(defn register-change
  [client path channel]
  (u/data client
          path
          (partial u/on-change client channel)))
