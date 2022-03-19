package Server;

import java.util.Comparator;

public class Server implements Comparable<Server> {

    private final String id;
    private final String address;
    private final int port;
    private final int clientPort;

    public Server(String id, String address, int port, int clientPort) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.clientPort = clientPort;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Server:" + id + " " + address + " " + port + " ";
    }

    @Override
    public int compareTo(Server server) {
        if (Integer.parseInt(this.getId()) == Integer.parseInt(server.getId())) {
            return 0;
        } else if (Integer.parseInt(this.getId()) > Integer.parseInt(server.getId())) {
            return 1;
        }
        return -1;
    }

    public int getClientPort() {
        return clientPort;
    }

    public static class ServerComparator implements Comparator<Server> {
        @Override
        public int compare(Server server, Server t1) {
            return server.compareTo(t1);
        }
    }

}

