import ClientHandler.ClientHandler;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        logger.info("Server Id: " + args[0] + "Conf file path:" + args[1]);
        ServerState.getServerState().initialize(args[0], args[1]);

        ServerSocket serverClientsSocket = new ServerSocket();
        SocketAddress endPointClient = new InetSocketAddress(
                ServerState.getServerState().getServerAddress(),
                ServerState.getServerState().getClientsPort());
        serverClientsSocket.bind(endPointClient);
        logger.debug(serverClientsSocket.getLocalSocketAddress());
        logger.info("Waiting for clients on port "+ serverClientsSocket.getLocalPort());
        System.out.println("Waiting for clients on port "+ serverClientsSocket.getLocalPort());

        while (true) {
            Socket clientSocket = serverClientsSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            ServerState.getServerState().addClientHandlerThreadToMap(clientHandler);
            System.out.println("Client Starting.");
            clientHandler.start();
        }
    }
}
