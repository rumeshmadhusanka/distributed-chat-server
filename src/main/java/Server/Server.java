package Server;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class Server implements Comparable<Server> {

    private final String id;
    private final String address;
    private final int port;

    public Server(String id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
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
    public int compareTo(Server server) {
        if (Integer.parseInt(this.getId()) == Integer.parseInt(server.getId())) {
            return 0;
        } else if (Integer.parseInt(this.getId()) > Integer.parseInt(server.getId())) {
            return 1;
        }
        return -1;
    }

    public static class ServerComparator implements Comparator<Server> {
        @Override
        public int compare(Server server, Server t1) {
            return server.compareTo(t1);
        }
    }

//    public static void main(String[] args) {
//        Server s1 = new Server("100","abc",1234);
//        Server s2 = new Server("101","pqr",12345);
//        Server s3 = new Server("105","pqr",12345);
//
//        Collection<Server> servers = List.of(s1,s2,s3);
//        Collection<Server> noServers = List.of();
//        System.out.println(s1.compareTo(s2));
//        ServerComparator sc = new ServerComparator();
//        System.out.println(sc.compare(s1,s2));
//        Server m1 = servers.stream().max(sc).stream().findFirst().orElse(null);
//        System.out.println(m1.id);
//        Server m2 = noServers.stream().max(sc).stream().findFirst().orElse(null);
//        System.out.println(m2);
//    }
}

