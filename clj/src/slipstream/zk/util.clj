(ns slipstream.zk.util
  (:require [zookeeper :as zk]
            [clojure.core.async :as a]
            [cheshire.core :as json])
  (:import (java.util UUID)))

(def port 2181)

(defn connect
  ([] (connect port))
  ([port]
   (zk/connect (str "127.0.0.1:" port))))

;; Utility to create znodes

(defn create-all-persistent
  ([client path]
   (create-all-persistent client path nil))
  ([client path watcher]
   (if watcher
     (zk/create-all client path :persistent? true :watcher watcher)
     (zk/create-all client path :persistent? true))))

(defn create-persistent
  ([client path]
   (create-persistent client path nil))
  ([client path watcher]
   (if watcher
     (zk/create client path :persistent? true :watcher watcher)
     (zk/create client path :persistent? true))))

(defn data
  [client path watcher]
  (zk/data client
           path
           :watcher watcher))

(defn exists
  ([client path]
    (exists client path nil))
  ([client path watcher]
   (if (nil? watcher)
     (zk/exists client
                path)
     (zk/exists client
                path
                :watcher watcher))))

(defn children
  [client path]
  (zk/children client path))

(defn delete-all
  [client path]
  (zk/delete-all client path))

;; Utility for manipulating znode data ;;

(defn get-version
  "Extract version from znode"
  [client path]
  (-> (zk/data client path) :stat :version))

(defn get-data
  "Get data, as string, from the znode"
  [client path]
  (let [data (:data (zk/data client path))]
  (when (-> data nil? not)
    (String. data))))

(defn set-data
  "Set string data in znode, ignoring the version"
  [client path value]
  (let [version (get-version client path)]
    (zk/set-data client path (.getBytes (str value) "UTF-8") version)))

(defn on-change
  "When the ZooKeeper event fires, check that the event was caused by a node change (data).
   Then extract the value from the znode, put it on the channel and re-register the watcher."
  [client channel {:keys [event-type path]}]
  ;  (println "on-change:" path event-type)
  (when (= event-type :NodeDataChanged)
    (let [value (get-data client path)]
      (a/go
        (a/>! channel (json/generate-string {:path path :value value})))
      ;   (println "after put in on-change:" path event-type)
      ))
  ;  (println "re-register watecher for:" path event-type)
  (zk/data client path :watcher (partial on-change client channel))
  ;(println "after re-register")
  )

;; General utility

(defn lazy-contains? [col k]
  (not (nil? (seq (filter #(= k %) col)))))

(defn uuid [] (str (UUID/randomUUID)))

;; Diagnostics / debugging ;;

(defn find-leaf-nodes [client path]
  (let [cs (children client path)]
    (if (seq? cs)
      (do
        (map #(find-leaf-nodes client (str path "/" %)) cs))
      (str path))))

(defn walk
  "Walk the entire tree and print each node"
  [client path]
  (sort (flatten (find-leaf-nodes client path))))

;(defn walk
;  "Walk the entire tree and print each node"
;  ([client path]
;   (walk client path {}))
;  ([client path res]
;   (when (zk/exists client path)
;     (println path))
;   (if-let [children (zk/children client path)]
;     ; call walk for all children znodes
;     (into res (map #(apply walk (conj % res)) (map vector (repeat client) (map #(str path "/" %) children)))))
;   res))
