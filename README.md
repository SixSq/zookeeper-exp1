# zookeeper-exp1

This repository is the result of experimentation with ZooKeeper to explore how to eliminate herding, loops and pulling in SlipStream.

The main namespaces are:

1. Clojure project including the following main namespaces to model the run state machine:
   a. zookeeper-exp1.buddy-circle: a collaborative system to ensure micro-services and join and leave a pool, without      needing external coordination in the process.
   b. zookeeper-exp1.run_state_machine: a reimplementation of the SlipStream run state machine, providing coordination     between the different VMs deployed.

2. Aleph web-socket service and a Python client example to create push notification when a run parameter value changes:
   a. zookeeper-exp1.service: a simple ring service, with the handler to register a ZooKeeper event corresponding to the
      requested run parameter, and chaining channels to deliver the event to the websocket client.
   b. a Python client to open a web socket on the right resource, waiting for the value of the corresponding resource
      to change.

The unit tests are only partial at this stage.

Support for both boot and leiningen is available.

TODO:

1. Extract the buddy-circle, such that other resources can take advantage of it, if needed.
2. Integrate with the configuration system of SlipStream, to retrieve for example ZooKeeper endpoints.

## Usage

Here's a quick primer to get you going:

1. Start ZooKeeper (after having installed it)  
`./bin/zkServer.sh start-foreground`

2. Create a run structure (the run id in this example is `run-1235`)  
`lein run "create run-1235 {\"node1\" [1 2 4] \"node2\" [2]}"`

3. View the node structure, in the repl  
`(use 'zookeeper-exp1.util :reload-all)(require '[zookeeper-exp1.util :as u])`  
`(use 'zookeeper-exp1.run-state-machine :reload-all)(require '[zookeeper-exp1.run-state-machine :as ss])`  
`(def client (u/connect))`  
`(ss/walk client "run-1235")`  

4. Launch a few instance of the main program  
`lein run "wait run-1235"`

5. Simulate that all VMs report completion, in the repl  
`(use 'zookeeper-exp1.run-state-machine :reload-all)(require '[zookeeper-exp1.run-state-machine :as ss])`  
`(use 'zookeeper-exp1.util :reload-all)(require '[zookeeper-exp1.util :as u])`  
`(def client (u/connect))`  
`(for [n ["node1" "node2"] i [1 2 4]]  
(ss/complete-single-state! client "n-0000000105" "run-1235" n i "init" "running"))`

And watch all main programs launched with the 'wait' command acknowledge the state transition. 

Voilà!

## License

Copyright © 2016 SixSq Sàrl

Distributed under the Apache 2.0 License.
