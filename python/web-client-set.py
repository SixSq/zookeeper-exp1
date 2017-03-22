import requests
value = 0
run_id = '192bed11-ed86-413a-a2fb-e2c2f648ffd4'
def set(v):
    r = requests.put('http://localhost:30001/runs/' + run_id + '/nodes/node1/1/p1',data=v)
    #r = requests.put('http://localhost:30001/toto',data=v)
    print "Status code: %s" % r.status_code
    print "Header: %s" % r.headers
    print "Value: %s" % r.text
#print "Value: %s" % r.json()
#while (value < 10):
while True:
    value += 1
    set({'value':value})
set({'value':value})
