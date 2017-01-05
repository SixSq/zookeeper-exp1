(ns zookeeper-exp1.util
  (:require [zookeeper :as zk]
            [zookeeper.util :as util]))

(def port 2181)

(defn connect
  ([] (connect port))
  ([port]
   (zk/connect (str "127.0.0.1:" port))))

;; Utility for manipulating znode data ;;

(defn get-version
  "Extract version from znode"
  [client path]
  (-> (zk/data client path) :stat :version))

(defn get-data
  "Get data, as string, from the znode"
  [client path]
  (String. (:data (zk/data client path))))

(defn set-data
  "Set string data in znode, ignoring the version"
  [client path value]
  (let [version (get-version client path)]
    (zk/set-data client path (.getBytes (str value) "UTF-8") version)))

;; General utility

(defn lazy-contains? [col k]
  (not (nil? (seq (filter #(= k %) col)))))
