(ns zookeeper-exp1.run-state-machine
  "This namespace uses ZooKeeper to create a distributed system to host the state of a run. Each VM is represented
  by an index node under its corresponding node. As each VM reports its current state completed, the corresponding
  index znode (i.e. ZooKeeper node) is removed from the structure.  When no node remains, it means all VMs have
  reported, which means the state machine can move to its next state.

  This design includes the following important goals:
  1. state completion should be idem potent.  This is important such that if the client has a doubt about the
     completness of its call to report completion, it can be performed again, without risk in changing the the state
     machine (e.g. reporting as completed the next state in the run)
  2. no pulling is required to get the next state transition, since the system includes a notification mechanism.
  3. the system is fast, with no loops nor expensive state investigation. It is even constant wrt the size of the run
  4. works in a share nothing pattern, meaning that this namespace can be distributed over the network. For this, the
     new buddy-circle namespace is used to ensure automatic recovery of partial transition, is case of failure.  This
     means this namespace can be packaged as a micro-service, and deployed/removed as required, with no special
     coordination required.

  The topology of the run is persisted as an edn structure inside the ./topology znode.  This can be changed as nodes
  come and go, for scalable deployments.

  The unit tests are only partial at this stage.

  The main program includes a few basic commands to simulate multi micro service deployment.

  To discover the internal structure of the state machine, the easiest way is to invoque the walk function.

  Here's a quick primer to get you going:

  1. Start ZooKeeper
  ./bin/zkServer.sh start-foreground

  2. Create a run structure (the run id in this example is 'run-1235')
  lein run \"create run-1235 {\\\"node1\\\" [1 2 4] \\\"node2\\\" [2]}\"

  3. View the node structure, in the repl
  (use 'zookeeper-exp1.util :reload-all)(require '[zookeeper-exp1.util :as u])
  (use 'zookeeper-exp1.run-state-machine :reload-all)(require '[zookeeper-exp1.run-state-machine :as ss])
  (def client (u/connect))
  (ss/walk client \"run-1235\")

  4. Launch a few instance of the main program
  lein run \"wait run-1235\"

  5. Simulate that all VMs report completion, in the repl:
  (use 'zookeeper-exp1.run-state-machine :reload-all)(require '[zookeeper-exp1.run-state-machine :as ss])
  (use 'zookeeper-exp1.util :reload-all)(require '[zookeeper-exp1.util :as u])
  (def client (u/connect))
  (for [n [\"node1\" \"node2\"] i [1 2 4]]
    (ss/complete-single-state! client \"n-0000000105\" \"run-1235\" n i \"1\" \"1\"))

  And watch all main programs launched with the 'wait' command acknowledge the state transition. Voilà!
  "
  (:require [zookeeper-exp1.util :as u]
            [zookeeper-exp1.buddy-circle :as bc]
            [zookeeper :as zk]
            [clojure.edn :as edn]))

(def root-znode-path "/runs")
(def state-znode-name "state")
(def transition-znode-name "transition")
(def topology-znode-name "topology")

;; Utility for znode names and paths ;;

(defn run-id-path [run-id]
  (str root-znode-path "/" run-id))

(defn- topology-znode-path [run-id]
  (str (run-id-path run-id) "/" topology-znode-name))

(defn- nodes-path [run-id]
  (str (run-id-path run-id) "/nodes"))

(defn- node-path [run-id node-name]
  (str (nodes-path run-id) "/" node-name))

(defn state-znode-path
  [run-id]
  (str (run-id-path run-id) "/" state-znode-name))

(defn run-id-from-path
  [path]
  (nth (clojure.string/split path #"/") 2))

(defn transition-znode-path
  [run-id]
  (str (run-id-path run-id) "/" transition-znode-name))

;; Accessor functions ;;

(defn- set-state!
  [client run-id s]
  (let [path (state-znode-path run-id)]
    (zk/create client path :persistent? true)
    (u/set-data client path s)))

(defn- store-nodes-topology
  "Store the node topology (map) in a specific znode"
  [client run-id nodes]
  (let [path (str (run-id-path run-id) "/" topology-znode-name)]
    (zk/create client path :persistent? true)
    (u/set-data client path (prn-str nodes))))

(defn- retrieve-nodes-topology
  "Retrieve the node topology as a map"
  [client run-id]
  (edn/read-string (u/get-data client (topology-znode-path run-id))))

;; Internal functions ;;

(defn- initialize-nodes
  "Create the nodes and indices"
  [client run-id nodes]
  (zk/create client (topology-znode-path run-id) :persistent? true)
  (doseq [n nodes]
    (let [node-name (node-path run-id (key n))]
      (zk/create-all client node-name :persistent? true)
      (doseq [i (val n)]
        (zk/create client (str node-name "/" i) :persistent? true)))))

(defn- transitioning?
  "Check if the state machine is in transition mode"
  [client run-id]
  (not (nil? (zk/exists client (transition-znode-path run-id)))))

(defn- check-transition-and-throw!
  "Throw if in transition"
  [client run-id]
  (let [t? (transitioning? client run-id)]
    (when t?
      (throw (Exception. "State machine in transition, come back later")))))

(declare get-run-state)

(defn- check-same-state-and-throw!
  "Throw if in transition"
  [client run-id state]
  (let [current-state (get-run-state client run-id)]
    (if-not (= state current-state)
      (throw (Exception.
               (str "State machine (state = " current-state ") in different state (requested = " state ")"))))))

(defn- start-transaction
  "Set znodes to indicate a transaction is taking place"
  [client watcher run-id]
  (bc/register-transation client watcher run-id))

(defn- end-transaction
  "Delete znodes to indicate a transaction is over"
  [client watcher run-id]
  (zk/delete client (transition-znode-path run-id))
  (bc/clear-transaction client watcher run-id))

(defn- create-topology
  "(Re)create topology"
  [client run-id nodes]
  (zk/create-all client (run-id-path run-id) :persistent? true)
  (store-nodes-topology client run-id nodes)
  (initialize-nodes client run-id nodes))

(defn complete-update
  [client watcher run-id]
  (create-topology client run-id (retrieve-nodes-topology client run-id))
  (let [state (u/get-data client (transition-znode-path run-id))]
    (end-transaction client watcher run-id)
    ; set the state after the transaction end, since watchers will be checking
    (set-state! client run-id state)))

(defn check-and-update-machine!
  "Since nodes are removed as they are completing the current state,
   all is done when no children of /runs/<run-id>/nodes are left."
  [client watcher run-id next-state]
  (if (nil? (zk/children client (nodes-path run-id)))
    (do
      (start-transaction client watcher run-id)
      ; store the next-state in the node, such that the transaction can be completed by the watcher in case the
      ; buddy goes away
      (zk/create client
                 (transition-znode-path run-id)
                 :persistent? true
                 :data (.getBytes next-state "UTF-8"))
      (complete-update client watcher run-id)
      true)
    false))

;; Public functions ;;

(defn get-run-state
  "Extract the :data (as string) of the /runs/<run-id>/state node"
  [client run-id]
  (check-transition-and-throw! client run-id)
  (u/get-data client (state-znode-path run-id)))

(defn reset-state-and-topology!
  "Reset the machine to a new state, optionaly altering the nodes topology,
   useful when scaling up or down.
   TODO: check if in the right state?"
  [client run-id nodes new-state]
  (check-transition-and-throw! client run-id)
  (zk/create-all client (run-id-path run-id) :persistent? true)
  (store-nodes-topology client run-id nodes)
  (initialize-nodes client run-id nodes)
  (set-state! client run-id new-state))

(defn create-run
  "Create run, fail if it already exists.
   Ex:
     (create-run <run-id> {\"node1\" [1 2 4] \"node2\" [2]})"
  ([client run-id nodes]
   (create-run client run-id nodes "init"))
  ([client run-id nodes initial-state]
   (when (zk/exists client (run-id-path run-id))
     (throw (Exception. (str "Run already exists! (" run-id ")"))))
   (reset-state-and-topology! client run-id nodes initial-state)))

(defn remove-run
  "Remove the run"
  [client run-id]
  (zk/delete-all client (root-znode-path run-id)))

(defn complete-single-state!
  "Complete the current node index for a given state, by deleting the znode.
   Fail if the global state doesn't match the given state.
   Check if all nodes have completed, and if so, update the global state.
   Return true is the state machine changed state, false otherwise.
   Note: this function is idem potent."
  [client watcher run-id node-name index current-state next-state]
  (check-transition-and-throw! client run-id)
  (check-same-state-and-throw! client run-id current-state)
  (let [node-path (node-path run-id node-name)]
    (zk/delete client (str node-path "/" index))
    (when-not (zk/children client node-path)
      (zk/delete client node-path)))
  (check-and-update-machine! client watcher run-id next-state))

(defn register-state-change
  [client f run-id]
  (let [path (state-znode-path run-id)]
    (zk/data client path :watcher f)))

;; Diagnostics / debugging ;;

(defn- _walk
  [client path]
  (when (zk/exists client path)
    (println path))
  (let [children (zk/children client path)]
    (when children
      ; call _walk for all children znodes
      (map #(apply _walk %) (map vector (repeat client) (map #(str path "/" %) children))))))

(defn walk
  "Walk the tree and print each node"
  [client run-id]
  (let [path (run-id-path run-id)]
    (_walk client path)))

(defn extract-topology
  [client run-id]
  (retrieve-nodes-topology client run-id))
