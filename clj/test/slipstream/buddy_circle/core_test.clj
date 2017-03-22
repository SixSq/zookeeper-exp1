(ns slipstream.buddy-circle.core-test
  (:require [slipstream.buddy-circle.core :refer :all]
            [clojure.test :refer :all]))

(deftest node-from-root-path-test
         (testing "test string result")
  (is (= "n-1234" (node-from-root-path "/buddy-circle/circle/nodes/n-1234"))))