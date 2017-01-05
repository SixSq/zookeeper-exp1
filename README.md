# zookeeper-exp1

This repository is the result of experimentation with ZooKeeper to explore how to eliminate herding, loops and pulling in SlipStream.

The main namspaces are:

1. zookeeper-exp1.buddy-circle: a collaborative system to ensure micro-services and join and leave a pool, without needing external coordination in the process.
2. zookeeper-exp1.run_state_machine: a reimplementation of the SlipStream run state machine, providing coordination between the different VMs deployed.

The unit tests are only partial at this stage.

Support for both boot and leiningen is available. 

TODO:

1. Hook-up with Aleph, such that state transitions can be delivered to clients via web-sockets. The main program includes a few basic commands to simulate multi micro service deployment.
2. Integrate with the configuration system of SlipStream, to retrieve for example ZooKeeper endpoints.

## Usage

Here's a quick primer to get you going:

1. Start ZooKeeper (after having installed it)
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

## License

Copyright © 2016 SixSq Sàrl

Distributed under the Apache 2.0 License.
