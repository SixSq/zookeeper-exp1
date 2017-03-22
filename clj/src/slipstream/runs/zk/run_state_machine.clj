(ns slipstream.runs.zk.run-state-machine
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
  (:require [slipstream.zk.util :as u]
            [slipstream.buddy-circle.core :as bc]
            [slipstream.runs.zk.util :as rzu]
            [slipstream.runs.zk.state :as s]
            [zookeeper :as zk]
            [clojure.edn :as edn]))

;; State machine states
(def initial-state "init")
(def valid-transitions
  {initial-state ["provisioning"]
   "provisioning" ["executing"]
   "executing" ["sending report"]
   "sending report" ["ready"]
   "ready" ["provisioning" ]
   "finalyzing" ["terminated"]
   "terminated" []})

;; Accessor functions ;;

(defn- store-nodes-topology
  "Store the node topology (map) in a specific znode"
  [client run-id nodes]
  (let [path (str (rzu/run-id-path run-id) "/" rzu/topology-znode-name)]
    (u/create-persistent client path)
    (u/set-data client path (prn-str nodes))))

(defn retrieve-nodes-topology
  "Retrieve the node topology as a map"
  [client run-id]
  (edn/read-string (u/get-data client (rzu/topology-znode-path run-id))))

;; Internal functions ;;

(defn- initialize-nodes
  "Create the nodes and indices"
  [client run-id nodes]
  (u/create-persistent client (rzu/topology-znode-path run-id))
  (doseq [n nodes]
    (let [node-name (rzu/rsm-node-path run-id (key n))]
      (u/create-all-persistent client node-name)
      (doseq [i (val n)]
        (u/create-persistent client (str node-name "/" i))))))

(defn- transitioning?
  "Check if the state machine is in transition mode"
  [client run-id]
  (not (nil? (zk/exists client (rzu/transition-znode-path run-id)))))

(defn check-transition-and-throw!
  "Throw if in transition"
  [client run-id]
  (let [t? (transitioning? client run-id)]
    (when t?
      (throw (Exception. "State machine in transition, come back later")))))

(defn- check-same-state-and-throw!
  "Throw if in transition"
  [client run-id state]
  (let [current-state (s/get- client run-id)]
    (if-not (= state current-state)
      (throw (Exception.
               (str "Current state (" current-state ") is different from expected by client (" state ")"))))))

(defn- start-transaction
  "Set znodes to indicate a transaction is taking place"
  [client me run-id]
  (bc/register-transation client me run-id))

(defn- end-transaction
  "Delete znodes to indicate a transaction is over"
  [client me run-id]
  (zk/delete client (rzu/transition-znode-path run-id))
  (bc/clear-transaction client me run-id))

(defn- create-topology
  "(Re)create topology"
  [client run-id nodes]
  (u/create-all-persistent client (rzu/run-id-path run-id))
  (store-nodes-topology client run-id nodes)
  (initialize-nodes client run-id nodes))

(defn- set-state!
  [client run-id s]
  (let [path (rzu/state-znode-path run-id)]
    (u/create-persistent client path)
    (u/set-data client path s)))

(defn complete-update
  [client me run-id]
  (create-topology client run-id (retrieve-nodes-topology client run-id))
  (let [state (u/get-data client (rzu/transition-znode-path run-id))]
    (end-transaction client me run-id)
    ; set the state after the transaction end, since watchers will be checking
    (set-state! client run-id state)))

(defn check-and-update-machine!
  "Since nodes are removed as they are completing the current state,
   all is done when no children of /runs/<run-id>/nodes are left."
  [client me run-id next-state]
  (if (nil? (zk/children client (rzu/rsm-nodes-path run-id)))
    (do
      (start-transaction client me run-id)
      ; store the next-state in the node, such that the transaction can be completed by the watcher in case the
      ; buddy goes away
      (zk/create client
                 (rzu/transition-znode-path run-id)
                 :persistent? true
                 :data (.getBytes next-state "UTF-8"))
      (complete-update client me run-id)
      true)
    false))

(defn extract-topology
  [client run-id]
  (retrieve-nodes-topology client run-id))

(defn module-to-nodes
  "Extract the nodes and build a map: <node> [1..multiplicity] for each node
  TODO: take into account params and also support app comp (not just app)"
  [module params]
  (into {} (map #(hash-map (key %) (vec (range 1 (inc (:multiplicity (val %)))))) (:nodes module))))

;; Public functions ;;

(defn reset-state-and-topology!
  "Reset the machine to a new state, optionaly altering the nodes topology,
   useful when scaling up or down.
   TODO: check if in the right state?"
  [client run-id nodes new-state]
  (check-transition-and-throw! client run-id)
  (u/create-all-persistent client (rzu/run-id-path run-id))
  (store-nodes-topology client run-id nodes)
  (initialize-nodes client run-id nodes)
  (set-state! client run-id new-state))

(defn create
  [client run-id module params]
  (let [nodes (module-to-nodes module params)]
    (reset-state-and-topology! client run-id nodes initial-state)))

(defn complete-single-state!
  "Complete the current node index for a given state, by deleting the znode.
   Fail if the global state doesn't match the given state.
   Check if all nodes have completed, and if so, update the global state.
   Return true is the state machine changed state, false otherwise.
   Note: this function is idem potent."
  [client me run-id node-name index current-state next-state]
  (check-transition-and-throw! client run-id)
  (check-same-state-and-throw! client run-id current-state)
  (let [node-path (rzu/rsm-node-path run-id node-name)]
    (zk/delete client (str node-path "/" index))
    (when-not (zk/children client node-path)
      (zk/delete client node-path)))
  (check-and-update-machine! client me run-id next-state))

(defn register-state-change
  [client f run-id]
  (let [path (rzu/state-znode-path run-id)]
    (zk/data client path :watcher f)))
