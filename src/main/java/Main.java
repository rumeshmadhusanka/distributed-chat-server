import ClientHandler.ClientHandler;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
	// write your code here
//        System.out.println("Hello world");
//        System.out.println(args[0]);
//        List<ServerConfig.ServerInfo> servers = ServerConfig.getConfig(args[0]);
//        System.out.println(servers);
        ClientHandler clientHandler = new ClientHandler();
        clientHandler.startServer();
    }
}
