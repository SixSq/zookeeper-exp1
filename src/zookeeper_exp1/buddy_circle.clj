(ns zookeeper-exp1.buddy-circle
  "This namespace creates a circle of ZooKeeper nodes, where each node looks after another node, called its 'buddy'.
  Nodes can come by simply calling 'join-circle', and go as they wish.  The joining node can also provide a special
  function which will be called when a buddy goes away (i.e. client dies or is disconnected from the ZooKeeper
  cluster). The buddy system (inspied from scuba diving) is in place, such that if the buddy is in trouble (i.e. goes
  away) it swork in progress can be picked-up by its watcher.

  The buddy system is created using a sequenced set of nodes (e.g. n-0000000001). Joining nodes are always added at
  the end of the sequence (i.e. head of the sequence with highest count). Nodes watch after their buddy as their
  predecessor. Buddies are watched over by their successor. This tail (first in the node sequence) of the node
  sequence wrappes around and has the head (last in the node sequence) as its buddy.

  As nodes come and go, the buddy system is updated to ensure each node has a buddy and that each buddy has a watcher.
  To do this, we take advantage of the :watcher attribute of ZooKeeper nodes (aka znode) and the event types (e.g.
  delete -> :NodeDeleted and change of its data value -> :NodeDataChanged).

  As a summary:
  1. When the buddy goes, this watcher covers for it, and then cover the buddy of the buddy instead.
  2. When a new node comes to town (always the last index), the first node makes the last its buddy."
  (:require [zookeeper-exp1.util :as u]
            [zookeeper :as zk]
            [zookeeper.util :as zutil]))

(def root-znode-path "/buddy-circle")
(def nodes-path (str root-znode-path "/nodes"))
(def transactions-znode-name "transactions")

(defn transactions-znode-path
  [watcher]
  (str root-znode-path "/" transactions-znode-name "/" watcher))

(defn node-from-root-path [path]
  (.substring path (inc (count nodes-path))))

(defn- cover-for-buddy
  "Whatever needs to be done when my buddy goes away..."
  [client me buddy clear-transactions-fn]
  (println "I'm" me "and I'm covering for my gone buddy (" buddy ")!")
  ;(clear-transactions-fn client buddy)
  )

(defn predecessor
  "Find predecessor, or last if me is first. Return nil if coll only contains one
   element"
  [client me coll]
  (when-not (or (not (u/lazy-contains? coll me)) (< (count coll) 2))
    (let [pred (ffirst (filter #(= (second %) me) (partition 2 1 coll)))]
      (if (nil? pred)
        (last coll)
        pred))))

(defn node-type-from-transaction-path
  "Nodes performing transaction work can inform its watcher that a transaction is in progress. This allows
   the watcher to complete the work in case the node is disconnected before completing the transaction.
   The trasaction is modelled using znodes, as follows:
    /buddy-circle/transactions/<buddy>/<node-type> for e.g. /buddy-circle/transactions/n-0000000001/runs
   Note that <buddy> is a sequenced znode corresponding to the node created when joining the circle.
   The transaction system is designed to be multi-purpose and work for different types of work. The example
   above is for the run state machine."
  [path]
  (nth (clojure.string/split path #"/") 3))

(defn get-nodes
  [client]
  (zutil/sort-sequential-nodes
    (zk/children client nodes-path)))

;; Public functions ;;

(defn get-open-transactions
  "Fetch all open transactions. node-type is the type of node we are interested in (e.g. \"runs\")."
  [client watcher node-type]
  (let [children (zk/children client (str (transactions-znode-path watcher) "/" node-type))]
    ; zk/children returns false is the znode doesn't exists
    (if (false? children)
      nil
      children)))

(defn- watch-buddy
  "When a buddy is deleted:
    1. complete its transaction, if any
    2. trigger the successor, who needs to close the gap on the predecesor of the buddy.
   A new node is always added at the head (last), therefore trigger the tail (first) node, such that it updates
   its buddy, since it now needs to point to the new head (last)."
  [client me buddy clear-transactions-fn {:keys [event-type path]}]
  (let [nodes (get-nodes client)]
    (println "\nevent-type:" event-type "path:" path "me:" me "buddy:" buddy)

    (when (and (= event-type :NodeDeleted) (= (node-from-root-path path) buddy))
      (cover-for-buddy client me buddy clear-transactions-fn)
      (let [new-buddy (predecessor client me nodes)]        ; buddy is already gone from nodes
        (if (nil? new-buddy)
          (println "My buddy is gone and I'm all alone :-(")
          (do
            (println "Now that my buddy is gone, I found a new buddy:" new-buddy)
            (zk/data client
                     (str nodes-path "/" new-buddy)
                     :watcher (partial watch-buddy client me new-buddy clear-transactions-fn))))))

    (when (and (= event-type :NodeDataChanged) (= (node-from-root-path path) me))
      (print "A new node joined. I'm the tail (" me "=" (first nodes) ") and the new node ")
      (let [new-buddy (last nodes)]
        (println new-buddy "is my new buddy.")
        ; register a watcher for my new buddy
        (zk/data
          client
          (str nodes-path "/" me)
          :watcher (partial watch-buddy client me new-buddy clear-transactions-fn))
        (zk/data
          client
          (str nodes-path "/" new-buddy)
          :watcher (partial watch-buddy client me new-buddy clear-transactions-fn))))

    (when (and (= event-type :NodeDataChanged) (= (node-from-root-path path) buddy))
      (println "A node changed but I don't have anything to do at this time. Reregistering for future changes.")
      (zk/data client path :watcher (partial watch-buddy client me buddy clear-transactions-fn)))))


;(defn successor
;  "Find successor, or first if me is last. Return nil if coll only contains one
;   element"
;  [client me coll]
;  (when-not (or (not (u/lazy-contains? coll me)) (< (count coll) 2))
;    (let [pred (last (last (filter #(= (first %) me) (partition 2 1 coll))))]
;      (if (nil? pred)
;        (first coll)
;        pred))))

(defn find-buddy-and-watch-over
  "Buddy is the one I look after (my predecessor)"
  [client me clear-transactions-fn]
  (let [nodes (get-nodes client)
        buddy (predecessor client me nodes)]
    (print "I am" me)
    (if (nil? buddy)
      (do
        (println " and I'm the first, so I don't have a buddy :-( (yet!)")
        ; register a watcher so that I can hook-up with my buddy when the next node joins the circle
        (zk/data client (str nodes-path "/" me) :watcher (partial watch-buddy client me nil clear-transactions-fn)))
      (do
        (println " and my buddy is:" buddy)
        ; register a watcher such that when my buddy goes, I can pick its work and find another buddy
        (zk/data client (str nodes-path "/" buddy) :watcher (partial watch-buddy client me buddy clear-transactions-fn))
        ; register a watcher so that I can hook-up with my buddy when the next node joins the circle
        (zk/data client (str nodes-path "/" me) :watcher (partial watch-buddy client me buddy clear-transactions-fn))
        ; trigger watcher on the first (tail) node, such that it can update its buddy since it always need to have the
        ; last (head) as a buddy.
        (u/set-data client (str nodes-path "/" (first nodes)) (str "last node: " me))))))

(defn join-circle
  [client clear-transactions-fn]
  (when-not (zk/exists client nodes-path)
    (zk/create client nodes-path :persistent? true))
  (let [me (node-from-root-path (zk/create client (str nodes-path "/n-") :sequential? true :ephemeral? true))]
    (find-buddy-and-watch-over client me clear-transactions-fn)))

(defn register-transation
  "Create a znode to signal the current transactions open by a given watcher (i.e. node).
   When the buddy system notices that a buddy is gone, this structure is used to detect
   transactions currently ongoing, such that they can be completed by the buddy."
  [client watcher item]
  (zk/create-all client (str (transactions-znode-path watcher) "/" item)))

(defn clear-transaction
  "Create a znode to signal the current transactions open by a given watcher (i.e. node).
   When the buddy system notices that a buddy is gone, this structure is used to detect
   transactions currently ongoing, such that they can be completed by the buddy.
   watcher is the sequenced node (me) and item the node (e.g. \"runs/run-1234\")."
  [client watcher item]
  (let [transactions-path (transactions-znode-path watcher)]
    (zk/delete client (str  transactions-path "/" item))
    (when (zk/exists client transactions-path)
      (zk/delete client transactions-path))))

;; Diagnostics / debugging ;;

(defn walk
  "Walk the entire tree and print each node"
  [client path]
  (println path)
  (let [children (zk/children client path)]
    (when children
      (map walk (map #(str path "/" %) children)))))
