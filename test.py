from io import TextIOWrapper

import pexpect

# pip install pexpect
import sys

num_servers = 3
num_clients = 3
servers = []
clients = []


def start_servers(n):
    for i in range(1, num_servers + 1):
        server = pexpect.spawn("java -jar target/ChatServer-1.0.0-jar-with-dependencies.jar s" + str(i) + " conf",
                               encoding='utf-8')
        server.logfile = sys.stdout
        servers.append(server)


def start_clients(n):
    for i in range(num_clients):
        client = pexpect.spawn('java -jar ChatClient.jar  --host localhost --port 4444 -i "Alice" -d', encoding='utf-8')
        client.logfile = sys.stdout
        clients.append(client)


try:
    start_servers(num_servers)
    start_clients(num_clients)
    clients[0].sendline("Hello")
    clients[0].expect("Hello")
finally:
    for s in servers:
        s.close(force=True)
    for c in clients:
        c.close(force=True)

if __name__ == '__main__':
    pass
