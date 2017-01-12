(ns zookeeper-exp1.main
"Examples:
lein run \"create run-1234 {\\\"node1\\\" [1 2 4] \\\"node2\\\" [2]}\"
lein run \"complete run-1234 node1 1 init running}\"
"
  (:require [zookeeper-exp1.run-state-machine :as rsm]
            [zookeeper-exp1.util :as u]
            [zookeeper-exp1.buddy-circle :as bc]
            [zookeeper :as zk]
            [clojure.edn :as edn]
            [clojure.string :as s]
            [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan alt! alts!! go]]))

(def changed? (atom false))

(defn on-state-changed
  [client channel {:keys [event-type path]}]
  (try
    (let [run-id (rsm/run-id-from-path path)
          state-path (rsm/run-id-path run-id)
          state (rsm/get-run-state client run-id)]
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
    (let [argv (s/split args #" ")
          cmd (first argv)
          arg (rest argv)
          client (u/connect)]
      (case cmd
        "create" (do
                   (println "Creating nodes...")
                   (let [run-id (first arg)
                         nodes  (edn/read-string (s/join " " (rest arg)))]
                     (rsm/create-run client run-id nodes)))
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
