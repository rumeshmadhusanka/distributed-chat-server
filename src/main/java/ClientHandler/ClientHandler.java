package ClientHandler;

import Consensus.Consensus;
import Constants.ChatServerConstants;
import Constants.ChatServerConstants.ClientConstants;
import Messaging.Messaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientHandler extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    final Object lock;
    private final Socket clientSocket;
    private boolean quitFlag;


    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lock = new Object();
    }

    @Override
    public void run() {
        try {
            // Start client handler and wait for client to connect.
            logger.info("Client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " connected.");
            //Create Input for the connection
            InputStream inputFromClient = clientSocket.getInputStream();
            Scanner scanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));

            while (!quitFlag) {
                String line = scanner.nextLine();
                logger.debug("Received: " + line);
                resolveClientRequest(Messaging.jsonParseRequest(line));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            logger.info("Connection has ended for client: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        }
    }

    public void resolveClientRequest(JSONObject jsonPayload) {
        String type = (String) jsonPayload.get("type");

        try {
            switch (type) {
                case ClientConstants.TYPE_CREATE_ID:
                    createNewIdentity((String) jsonPayload.get(ClientConstants.IDENTITY));

                case ClientConstants.TYPE_CREATE_ROOM:
                    //TODO: Implement new room logic

                case ClientConstants.TYPE_DELETE_ROOM:

                case ClientConstants.TYPE_JOIN_ROOM:

                case ClientConstants.TYPE_LIST:

                case ClientConstants.TYPE_MESSAGE:

                case ClientConstants.TYPE_MOVE_JOIN:

                case ClientConstants.TYPE_QUIT:

                case ClientConstants.TYPE_WHO:


            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        } catch (ParseException e) {
            logger.info(e.getMessage());
        }
    }

    public void createNewIdentity(String identity) throws IOException, ParseException {
        //TODO: Implement new identity logic.

        // Verify identity.
        boolean isAvailable = Consensus.verifyUniqueValue(identity, ChatServerConstants.ServerConstants.IDENTITY);

        // Create identity.
        // Broadcast identity to other servers.
        // Send appropriate response.
        // move client to mainHall.
        JSONObject response;
        response = new JSONObject();
        response.put(ClientConstants.TYPE, ClientConstants.TYPE_CREATE_ID);
        response.put(ClientConstants.APPROVED, ClientConstants.TRUE);
        Messaging.respond(response, this.clientSocket);
    }
}
