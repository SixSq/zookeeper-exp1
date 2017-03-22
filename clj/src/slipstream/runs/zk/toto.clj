(ns slipstream.runs.zk.toto
  (:require [slipstream.zk.util :as u]
            [slipstream.runs.zk.util :as zu]
            [zookeeper :as zk]))

(zk/create @zu/client "/toto")

(defn register-change
  [client path channel]
  (zk/data client
           path
           :watcher (partial u/on-change client channel)))
