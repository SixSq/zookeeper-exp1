(ns slipstream.runs.zk.util
  (:require [slipstream.zk.util :as u]))

(def client (atom nil))
;(def watcher (atom (bc/join-circle client rsm/complete-update)))

(defn connect
  ([] (connect (u/connect)))
  ([c] (reset! client c)))

;; Utility for znode names and paths ;;

; Run znodes

(defn run-id-from-path
  [path]
  (nth (clojure.string/split path #"/") 2))

(def runs-znode-path "/runs")

(defn run-znode-path
  [run-id]
  (str runs-znode-path "/" run-id))

(defn run-id-path [run-id]
  (str runs-znode-path "/" run-id))

; Run nodes znodes

(defn nodes-znode-path
  [run-id]
  (str (run-znode-path run-id) "/nodes"))

(defn node-znode-path
  [run-id node]
  (str (nodes-znode-path run-id) "/" node))

(defn node-index-znode-path
  [run-id node index]
  (str (node-znode-path run-id node ) "/" index))

; Run params znodes

(defn params-znode-path
  [run-id node index]
  (str (node-index-znode-path run-id node index) "/params"))

(defn param-znode-path
  [run-id node index param]
  (str (params-znode-path run-id node index) "/" param))

; Run state machine znodes

(def transition-znode-name "transition")
(def topology-znode-name "topology")

(defn topology-znode-path [run-id]
  (str (run-id-path run-id) "/" topology-znode-name))

(defn rsm-nodes-path [run-id]
  (str (run-id-path run-id) "/state-machine-nodes"))

(defn rsm-node-path [run-id node-name]
  (str (rsm-nodes-path run-id) "/" node-name))

; Run state znodes

(def state-znode-name "state")

(defn state-znode-path
  [run-id]
  (str (run-znode-path run-id) "/" state-znode-name))

(defn transition-znode-path
  [run-id]
  (str (run-id-path run-id) "/" transition-znode-name))

;; Diagnostics

(defn walk-run
  [client run-id]
  (u/walk client (run-znode-path run-id)))
