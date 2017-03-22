(ns slipstream.runs.resources.param
  (:require [slipstream.runs.zk.param :as p]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.resources.util :as ru]
            [slipstream.zk.util :as u]
            [aleph.http :as http]
            [manifold.stream :as s]
            [clojure.core.async :as a]))

(defn set-
  "Set value of the parameter"
  [client req]
  (let [params (:params req)
        {:keys [run-id node index param]} params
        {:strs [value]} params]
    (p/set- client run-id node index param value)))

(defn get-handler
  [client req]
  (let [{{:keys [run-id node index param]} :params} req
        path (rzu/param-znode-path run-id node index param)]
    (if (ru/websocket? req)
      (if-let [socket (try
                        @(http/websocket-connection req)
                        (catch Exception e nil))]
        ;Pull from the request the run-id and param tuples from the params map
        (let [c (a/chan)]
          (p/register-change client path c)
          (s/connect c socket))
        (do
          (println "this is not websocket connection")
          ru/non-websocket-request)))
    (do
      ;TODO add support for json
      (ru/response "text/plain" (u/get-data client path)))))
