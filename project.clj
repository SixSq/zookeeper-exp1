(defproject zookeeper-exp1 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]
                 [zookeeper-clj "0.9.4"]
                 [org.apache.curator/curator-test "2.8.0" :scope "test"]]
  :main zookeeper-exp1.main)
