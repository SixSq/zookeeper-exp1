(ns slipstream.main
"Examples:
lein run \"create run-1234 {\\\"node1\\\" [1 2 4] \\\"node2\\\" [2]}\"
lein run \"complete run-1234 node1 1 init running}\"
"
  (:require [slipstream.runs.resources.run :as rr]
            [slipstream.runs.zk.run :as r]
            [slipstream.runs.zk.state :as s]
            [slipstream.runs.zk.run-state-machine :as rsm]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.zk.util :as u]
            [slipstream.buddy-circle.core :as bc]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan alt! alts!! go]]))

(def changed? (atom false))

(defn on-state-changed
  [client channel {:keys [event-type path]}]
  (try
    (let [run-id (rzu/run-id-from-path path)
          state-path (rzu/run-id-path run-id)
          state (s/get- client run-id)]
      (>!! channel state))
    (catch Exception e (prn "caught exception: " (.getMessage e)))))

(defn wait-for-state-change
  [client run-id]
  (let [c (chan)]
    (rsm/register-state-change client (partial on-state-changed client c) run-id)
    (while (false? @changed?)
      (let [[state channel] (alts!! [c (timeout 1000)])]
        (if state
          (do
            (println "\nState changed to:" state)
            (reset! changed? true))
          (do (print ".")
              (flush)))))))

(defn -main
  "Start a node and register a buddy."
  [& [args]]
  (when args
    (let [argv (str/split args #" ")
          cmd (first argv)
          arg (rest argv)
          client (u/connect)]
      (case cmd
        "create" (do
                   (println "Creating nodes...")
                   (let [run-id (first arg)
                         nodes  (edn/read-string (str/join " " (rest arg)))]
                     (rr/create client run-id nodes)))
        "walk" (do
                 (println "Printing run...")
                 (let [run-id (first arg)]
                   (rzu/walk-run client run-id)))
        "complete" (do
                     (println "Completing node state...")
                     (let [run-id (first arg)
                           node (second arg)
                           index (nth arg 2)
                           current-state (nth arg 3)
                           new-state (nth arg 4)
                           watcher (bc/join-circle client rsm/complete-update)
                           completed? (rsm/complete-single-state!
                                        client watcher run-id node index current-state new-state)]
                       (if completed? (println "completed!") (println "not completed"))))
        "wait" (do
                 (println "Waiting for state change...")
                 (let [run-id (first arg)]
                   (wait-for-state-change client run-id))))))
  (println "Done!"))
