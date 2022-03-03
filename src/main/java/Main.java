import ClientHandler.ClientHandler;
import Constants.ChatServerConstants.ServerConstants;
import Server.Room;
import Server.ServerHandler;
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

        // Initialize server state.
        logger.info("Server Id: " + args[0] + "Conf file path:" + args[1]);
        ServerState.getServerState().initialize(args[0], args[1]);

        // Add Servers from Conf file.
//        ServerState.getServerState().addServers();

        ServerState.getServerState().addRoomToMap(new Room(args[0],ServerConstants.MAIN_HALL+"-"+args[0]));

        // ServerSocket for coordination.
        ServerSocket serverCoordinationSocket = new ServerSocket();
        SocketAddress coordinationEndpoint = new InetSocketAddress(
                ServerState.getServerState().getServerAddress(),
                ServerState.getServerState().getCoordinationPort());
        serverCoordinationSocket.bind(coordinationEndpoint);
        logger.debug("Local Coordination Socket Address: " +serverCoordinationSocket.getLocalSocketAddress());
        logger.info("Listening on coordination port: " + serverCoordinationSocket.getLocalPort());

        // Start ServerHandler.
        ServerHandler serverHandler = new ServerHandler(serverCoordinationSocket);
        serverHandler.start();

        // ServerSocket for client communication.
        ServerSocket serverClientSocket = new ServerSocket();
        SocketAddress clientEndpoint = new InetSocketAddress(
                ServerState.getServerState().getServerAddress(),
                ServerState.getServerState().getClientsPort());
        serverClientSocket.bind(clientEndpoint);
        logger.debug("Local Client Socket Address: " +serverClientSocket.getLocalSocketAddress());
        logger.info("Waiting for clients on port "+ serverClientSocket.getLocalPort());

        // Start ClientHandler.
        while (true) {
            Socket clientSocket = serverClientSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            ServerState.getServerState().addClientHandler(clientHandler);
            logger.debug("Starting client handler.");
            clientHandler.start();
        }
    }
}
