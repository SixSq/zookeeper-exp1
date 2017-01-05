(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/core.async "0.2.395"]
                  [zookeeper-clj "0.9.4"]
  				  [org.apache.curator/curator-test "2.8.0" :scope "test"]
  				  [tolitius/boot-check "0.1.4"]])

(require '[tolitius.boot-check :as check])

(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-yagni)
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed :options {:verbose true
                               :max-line-length 120})))