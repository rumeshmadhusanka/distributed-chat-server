import ClientHandler.ClientHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
	// write your code here
//        System.out.println("Hello world");
//        System.out.println(args[0]);
//        List<ServerConfig.ServerInfo> servers = ServerConfig.getConfig(args[0]);
//        System.out.println(servers);
        logger.info("Hello");
        ClientHandler clientHandler = new ClientHandler();
//        clientHandler.startServer();
        System.out.println("Hello world!");
    }
}
