package ClientHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import Consensus.Consensus;
import Constants.ChatServerConstants;
import Constants.ChatServerConstants.ClientConstants;
import Messaging.Messaging;
import Server.Room;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import Server.ServerState;
import Exception.ServerException;


public class ClientHandler extends Thread{

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    final Object lock;
    private boolean quitFlag;


    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lock = new Object();
    }

    @Override
    public void run() {
        try {
            // Start client handler and wait for client to connect.
            logger.info("Client " + clientSocket.getInetAddress() + ":"+ clientSocket.getPort() + " connected.");
            //Create Input for the connection
            InputStream inputFromClient = clientSocket.getInputStream();
            Scanner scanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));

            while(!quitFlag) {
                String line = scanner.nextLine();
                logger.debug("Received: " + line);
                resolveClientRequest(Messaging.jsonParseRequest(line));
            }
        } catch (IOException | ParseException e) {
            logger.debug("Exception occurred " + e);
        } finally {
            logger.info("Connection has ended for client: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        }
    }

    public void resolveClientRequest(JSONObject jsonPayload) {
        String type = (String) jsonPayload.get("type");
        JSONObject response;

        String identity;
        try {
            switch (type) {
                case ClientConstants.TYPE_CREATE_ID:
                    response = new JSONObject();
                    identity = (String) jsonPayload.get(ClientConstants.IDENTITY);
                    if ((identity.length() > 3) && (identity.length() <= 16) && Character.isLetter(identity.charAt(0))) {
                        createNewIdentity(identity);

                    } else {
                        response.put(ClientConstants.TYPE, type);
                        response.put(ClientConstants.APPROVED, ClientConstants.FALSE);
                    }
                    Messaging.respond(response, this.clientSocket);

                case ClientConstants.TYPE_CREATE_ROOM:
                    String roomId = (String) jsonPayload.get(ClientConstants.ROOM_ID);
                    //TODO: Implement new room logic
                    response = new JSONObject();
                    response.put(ClientConstants.TYPE, type);
                    response.put(ClientConstants.ROOM_ID, roomId);
                    response.put(ClientConstants.APPROVED, ClientConstants.TRUE);
                    Messaging.respond(response, this.clientSocket);

                case ClientConstants.TYPE_DELETE_ROOM:

                case ClientConstants.TYPE_JOIN_ROOM:

                case ClientConstants.TYPE_LIST:

                case ClientConstants.TYPE_MESSAGE:

                case ClientConstants.TYPE_MOVE_JOIN:

                case ClientConstants.TYPE_QUIT:
                    this.quitFlag = true;

                case ClientConstants.TYPE_WHO:

            }
        } catch (IOException | ServerException | ParseException e) {
            logger.info(e.getMessage());
        }
    }

        /**
         * Create a new identity.
         *
         * @param identity - New identity received from the client.
         * @throws IOException
         * @throws ParseException
         */
        public void createNewIdentity(String identity) throws ServerException, IOException, ParseException {
            //TODO: Implement new identity logic.
            JSONObject response;

            // Verify identity.
            boolean isAvailable = Consensus.verifyUniqueValue(identity, ChatServerConstants.ServerConstants.IDENTITY);
            if (isAvailable) {
                // Create identity.
                ServerState.getServerState().addIdentity(identity);
            }
            Messaging.broadcastToPreviousRoom();
            // Broadcast identity to other servers.
            //Messaging.broadcastClients(new JSONObject());
            // Send appropriate response.
            // move client to mainHall.
            ServerState.getServerState().getRoom(ChatServerConstants.ServerConstants.MAIN_HALL).addClientIdentity(identity);

            response = new JSONObject();
            response.put(ClientConstants.TYPE, ClientConstants.TYPE_CREATE_ID);
            response.put(ClientConstants.APPROVED, ClientConstants.TRUE);
            Messaging.respond(response, this.clientSocket);
        }


    }
