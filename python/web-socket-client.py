from websocket import create_connection
run_id = '192bed11-ed86-413a-a2fb-e2c2f648ffd4'
ws = create_connection('ws://localhost:30001/runs/' + run_id + '/nodes/node1/1/p1')
#ws = create_connection("ws://localhost:30001/toto")
print "Receiving..."
while True:
    result =  ws.recv()
    print "Received '%s'" % result
result =  ws.recv()
print "Received '%s'" % result
ws.close()
