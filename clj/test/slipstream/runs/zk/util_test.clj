(ns slipstream.runs.zk.util-test
  (require [slipstream.zk.util :as u]
           [slipstream.runs.zk.util :as rzu]
           [slipstream.zk.util-test :as ut]
           [clojure.string :as str]))

(def module-a
  {:nodes {"node1" {:params {"p1" {:default "default-value-p1" :desc "description for p1"}
                             "p2" {:default "default-value-p2" :desc "description for p2"}
                             "p3" {:desc "description for p3"}}
                    :mapping {"p1" "node2.p4"}
                    :multiplicity 3}
           "node2" {:params {"p4" {:default "default-value-p4" :desc "description for p4"}
                             "p5" {:default "default-value-p5" :desc "description for p5"}
                             "p6" {:desc "description for p6"}}
                    :mapping {"p6" "node1.p1"}
                    :multiplicity 2}}})

(defn print-run
  [run-id]
  (println (str/join "\n" (rzu/walk-run @ut/client run-id))))
