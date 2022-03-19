package Consensus;

import Server.Server;

public class Leader extends Server {

    public Leader(Server server) {
        super(server.getId(), server.getAddress(), server.getPort(), server.getClientPort());
    }
}
