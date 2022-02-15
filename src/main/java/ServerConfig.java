import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerConfig {
    public static class ServerInfo {
        private final String serverId;
        private final String serverAddress;
        private final int serverPort;
        private final int coordinationPort;

        public ServerInfo(String serverId, String serverAddress, int serverPort, int coordinationPort) {
            this.serverId = serverId;
            this.serverAddress = serverAddress;
            this.serverPort = serverPort;
            this.coordinationPort = coordinationPort;
        }

        @Override
        public String toString() {
            return "ServerInfo{" +
                    "serverId='" + serverId + '\'' +
                    ", serverAddress='" + serverAddress + '\'' +
                    ", serverPort=" + serverPort +
                    ", coordinationPort=" + coordinationPort +
                    '}';
        }
    }

    private static final List<ServerInfo> servers = new ArrayList<>();

    public static List<ServerInfo> getConfig(String path) {
        File file = new File(path);
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String[] components = scanner.nextLine().split("\t");
                servers.add(new ServerInfo(components[0], components[1], Integer.parseInt(components[2]), Integer.parseInt(components[3])));
            }
            return servers;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return servers;
    }

    public static void main(String[] args) {

    }
}