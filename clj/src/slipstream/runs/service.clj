(ns slipstream.runs.service
  "This namespace builds a simple websocket server, listening for request for change on the
  state of a run.  This state is represented by a ZooKeeper node.
  Run has the following structure:
  state-machine (se run-state-machine namespace
  nodes (no value)
  nodes/:node (value: module ref, cloud offer and params list (what about vertical scaling?)
  nodes/:node/:index (no value)
  nodes/:node/:index/:param (value is the value :-))"
  (:require
    [slipstream.runs.resources.run :as rr]
    [slipstream.runs.resources.nodes :as rns]
    [slipstream.runs.resources.node :as rn]
    [slipstream.runs.resources.index :as ri]
    [slipstream.runs.resources.param :as rp]
    [slipstream.runs.resources.state :as rs]
    ;    [slipstream.runs.resources.toto :as toto]
    [slipstream.runs.zk.util :as zu]
    [aleph.http :as http]
    [ring.middleware.params :as params]
    [compojure.core :as compojure :refer [GET PUT POST DELETE]]
    [compojure.route :as route]))

(zu/connect)

(def handler
  (params/wrap-params
    (compojure/routes
      ;(GET "/toto" [] (partial toto/toto @zu/client))
      ;(PUT "/toto" [] (partial toto/toto-set @zu/client))
      (GET "/runs/:run-id/state" [] (partial rs/handler @zu/client))
      (GET "/runs/:run-id/nodes/:node/:index/:param" [] (partial rp/get-handler @zu/client))
      (PUT "/runs/:run-id/nodes/:node/:index/:param" [] (partial rp/set- @zu/client))
      (GET "/runs/:run-id/nodes/:node/:index" [] (partial ri/get-handler @zu/client))
      (POST "/runs/:run-id/nodes/:node/:index" [] (partial rs/complete-state-handler @zu/client))
      ;(POST "/runs/:run-id/nodes/:node" [] (partial rn/scale-handler @zu/client))
      (DELETE "/runs/:run-id" [] (partial rr/delete @zu/client))
      (POST "/runs" [] (partial rr/create-handler @zu/client))
      ;;TODO: add/remove node...
      (route/not-found "No such page."))))

(declare s)

(try (.close s) (catch Exception e nil))

(def s (http/start-server handler {:port 30001}))
