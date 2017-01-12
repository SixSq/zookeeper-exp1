(set-env!
  :resource-paths #{"src"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [zookeeper-clj "0.9.4"]
                 [org.apache.curator/curator-test "2.8.0" :scope "test"]
               	 [aleph "0.4.2-alpha1"]
                 [gloss "0.2.6"]
   		           [clj-tuple "0.2.2"]
                 [compojure "1.3.3"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]])

(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed :options {:verbose true
                               :max-line-length 120})))