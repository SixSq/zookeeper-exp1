(ns slipstream.runs.resources.toto
  (:require
    [slipstream.runs.zk.toto :as zt]
    [slipstream.runs.resources.util :as ru]
    [slipstream.runs.zk.util :as zu]
    [slipstream.zk.util :as u]
    [aleph.http :as http]
    [manifold.stream :as s]
    [manifold.bus :as bus]
    [clojure.core.async :as a]))

(defn toto
  [client req]
  ;  (println req)
  (if (ru/websocket? req)
    (let [{{:keys [run-id node index param]} :params} req
          path "/toto"]
      (if-let [socket (try
                        @(http/websocket-connection req)
                        (catch Exception e
                          (println "caught exception: " (.getMessage e))))]
        ;Pull from the request the run-id and param tuples from the params map
        (let [c (a/chan)]
          (println "this is a web socket")
          (zt/register-change client path c)
          (print "trying connect")
          (s/connect c socket {:downstream? false}))
        (do
          (println "this is not websocket connection")
          ru/non-websocket-request)))
    (u/get-data client "/toto")))

(defn toto-set
  [client req]
  (let [params (:params req)
        {:keys [run-id node index param]} params
        {:strs [value]} params
        path "/toto"]
    (u/set-data client path value)))
