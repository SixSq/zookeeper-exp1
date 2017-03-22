(ns slipstream.runs.resources.run
  "Model the run, as a set of znodes:
  /runs/:run-id/
    state-machine-nodes (see dedicated namespace)
    state
    nodes/:node/:index/:param"
  (:require [slipstream.runs.zk.run :as r]
            [slipstream.runs.zk.node :as n]
            [slipstream.runs.zk.param :as p]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]))

(defn create
  "Create run from module and params map
   module: map of {nodes {<node> {:params {<param> [default-value description]}
                                  :mapping {<param> [mapped-param, ]}}
   params: map of {nodes {<node> {multiplicity:value, cloud-offer:value, params}}}"
  [client module params]
  (r/create client module params))

(defn create-handler
  "Create a new run from a reference module (app or app comp).
   TODO: create state-machine
   TODO: extract structure"
  [client req]
  (let [{{:keys [module params]} :params} req]
    (create client module params)))

(defn delete
  "Remove the run"
  [client run-id]
  (u/delete-all client (rzu/run-znode-path run-id)))
