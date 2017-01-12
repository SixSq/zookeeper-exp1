(ns zookeeper-exp1.service
  "This namespace builds a simple websocket server, listening for request for change on the
  state of a run.  This state is represented by a ZooKeeper node."
(:require
  [compojure.core :as compojure :refer [GET]]
  [ring.middleware.params :as params]
  [compojure.route :as route]
  [aleph.http :as http]
  [manifold.stream :as s]
  [manifold.deferred :as d]
  [manifold.bus :as bus]
  [clojure.core.async :as a]
  [zookeeper :as zk]))

(def non-websocket-request
{:status 400
 :headers {"content-type" "application/text"}
 :body "Expected a websocket request."})

(def client (atom nil))

(defn on-change
  "When the ZooKeeper event fires, check that the event was caused by a node change (data).
   Then extract the value from the znode and put it on the channel."
  [client channel {:keys [event-type path]}]
  (when (= event-type :NodeDataChanged)
    (let [value (String. (:data (zk/data client path)))]
      (a/go
        (a/>! channel value)))))

(defn on-state-change-handler
  [req]
  (if-let [socket (try
                    @(http/websocket-connection req)
                    (catch Exception e
                      nil))]
    ;Pull from the request the run-id and param tuples from the params map
    (let [{{:keys [run-id param]} :params} req
          c (a/chan)]
      (zk/data @client
               (str "/runs/" run-id "/" param)
               :watcher (partial on-change @client c))
      (s/connect c socket))
    non-websocket-request))

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/runs/:run-id/:param" [] on-state-change-handler)
      (route/not-found "No such page."))))

(def s (http/start-server handler {:port 30001}))
