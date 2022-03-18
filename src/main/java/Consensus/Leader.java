package Consensus;

import Server.Server;

public class Leader extends Server {
    public Leader(String id, String address, int port, int clientPort) {
        super(id, address, port, clientPort);
    }

    public Leader(Server server) {
        super(server.getId(), server.getAddress(), server.getPort(), server.getClientPort());
    }
}
