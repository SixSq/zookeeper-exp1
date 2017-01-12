from websocket import create_connection
ws = create_connection("ws://localhost:30001/runs/run-1235/state")
print "Receiving..."
result =  ws.recv()
print "Received '%s'" % result
ws.close()
