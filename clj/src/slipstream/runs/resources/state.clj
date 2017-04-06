(ns slipstream.runs.resources.state
  (:require [slipstream.runs.zk.run :as r]
            [slipstream.runs.zk.param :as p]
            [slipstream.runs.zk.run-state-machine :as rsm]
            [slipstream.runs.zk.state :as s]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.resources.util :as ru]
            [slipstream.zk.util :as u]
            [aleph.http :as http]
            [manifold.stream :as stream]
            [clojure.core.async :as a]))

(defn get-
  [client run-id]
  (rsm/check-transition-and-throw! client run-id)
  (s/get- client run-id))

(defn handler
  [client req]
  (let [{{:keys [run-id node index param]} :params} req
        path (rzu/state-znode-path run-id)]
    (if (ru/websocket? req)
      (if-let [socket (try
                        @(http/websocket-connection req)
                        (catch Exception e nil))]
        ;Pull from the request the run-id and param tuples from the params map
        (let [c (a/chan)]
          (s/register-change client path c)
          (stream/connect c socket))
        (do
          ru/non-websocket-request))
      (do
        ;TODO add support for json
        (ru/response "text/plain" (get- client run-id))))))

(defn complete-state-handler
  "Complete current VM state"
  [client watcher req]
  (let [{{:keys [run-id node index]} :params} req
        current-state "init"
        next-state "init"]
    (rsm/complete-single-state! client watcher run-id node index current-state next-state)))

(defn set-
  [client req]
  (let [params (:params req)
        {:keys [run-id node index param]} params
        {:strs [value]} params
        path (rzu/param-znode-path run-id node index param)]
    (u/set-data client path value)))
