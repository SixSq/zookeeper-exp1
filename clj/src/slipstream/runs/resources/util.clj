(ns slipstream.runs.resources.util)

(defn response
  "Build a response based on the requested content-type and value"
  ([content-type value] (response content-type value 200))
  ([content-type value code]
   {:status code
    :headers {"Content-Type" content-type}
    :body value}))

(defn websocket?
  "Test if the request is a websocket upgrade"
  [req]
  (= "websocket" (-> req :headers :upgrade)))

(def non-websocket-request
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected a websocket request."})
