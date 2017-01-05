# zookeeper-exp1

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

(require '[zookeeper :as zk])
(use 'zookeeper-exp1.util :reload-all)(require '[zookeeper-exp1.util :as u])

(use 'zookeeper-exp1.buddy-circle :reload-all)(require '[zookeeper-exp1.buddy-circle :as bc])

(use 'zookeeper-exp1.run-state-machine :reload-all)(require '[zookeeper-exp1.run-state-machine :as ss])

(def client (u/connect))
