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

        ServerSocket serverClientSocket = new ServerSocket();
        SocketAddress endPointClient = new InetSocketAddress(
                ServerState.getServerState().getServerAddress(),
                ServerState.getServerState().getClientsPort());
        serverClientSocket.bind(endPointClient);
        logger.debug(serverClientSocket.getLocalSocketAddress());
        logger.info("Waiting for clients on port "+ serverClientSocket.getLocalPort());

        while (true) {
            Socket clientSocket = serverClientSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            ServerState.getServerState().addClientHandlerThreadToMap(clientHandler);
            logger.debug("Starting client handler.");
            clientHandler.start();
        }
    }
}
