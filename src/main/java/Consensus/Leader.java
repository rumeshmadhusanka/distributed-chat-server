package Consensus;

import Server.Server;

public class Leader extends Server {
    public Leader(String id, String address, int port) {
        super(id, address, port);
    }

    public Leader(Server server){
        super(server.getId(),server.getAddress(),server.getPort());
    }
}
