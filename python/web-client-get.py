import requests
run_id = '192bed11-ed86-413a-a2fb-e2c2f648ffd4'
def get():
    r = requests.get('http://localhost:30001/runs/' + run_id + '/nodes/node1/1/p1')
    #r = requests.get('http://localhost:30001/toto')
    print "Status code: %s" % r.status_code
    print "Header: %s" % r.headers
    print "Value: %s" % r.text
#print "Value: %s" % r.json()
# while True:
#     get()
get()
