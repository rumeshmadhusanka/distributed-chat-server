package ClientHandler;

import Consensus.Consensus;
import Constants.ChatServerConstants;
import Constants.ChatServerConstants.ClientConstants;
import Exception.ServerException;
import Messaging.Messaging;
import Server.Room;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
        } catch (IOException | ParseException | ServerException | InterruptedException e) {
            logger.info(e.getMessage());
        }
    }

    /**
     * Resolve a given request.
     *
     * @param jsonPayload -  Request as a JSONObject.
     */
    public void resolveClientRequest(JSONObject jsonPayload) throws ServerException, IOException, ParseException, InterruptedException {
        String type = (String) jsonPayload.get("type");


        switch (type) {
            case ClientConstants.TYPE_CREATE_ID:
                logger.debug("Resolving create new identity.");
                String identity = (String) jsonPayload.get(ClientConstants.IDENTITY);
                logger.debug("New identity: " + identity);
                if ((identity.length() > 3) && (identity.length() <= 16) && Character.isLetter(identity.charAt(0))) {
                    createNewIdentity(identity);

                } else {
                    JSONObject response = buildApprovedJSON(type, ClientConstants.FALSE);
                    Messaging.respond(response, this.clientSocket);
                }
                break;

            case ClientConstants.TYPE_CREATE_ROOM:
                logger.debug("Resolving create new room.");
                String room = (String) jsonPayload.get(ClientConstants.IDENTITY);
                logger.debug("New room: " + room);
                if ((room.length() > 3) && (room.length() <= 16) && Character.isLetter(room.charAt(0))) {
                    createNewRoom(room);

                } else {
                    JSONObject response = buildApprovedJSON(type, ClientConstants.FALSE);
                    Messaging.respond(response, this.clientSocket);
                }
                break;

            case ClientConstants.TYPE_DELETE_ROOM:
                logger.debug("Deleting room");
                break;

            case ClientConstants.TYPE_JOIN_ROOM:
                logger.debug("Joining room");
                break;

            case ClientConstants.TYPE_LIST:
                logger.debug("Sending users in room");
                break;

            case ClientConstants.TYPE_MESSAGE:
                logger.debug("Message received");
                break;

            case ClientConstants.TYPE_MOVE_JOIN:
                logger.debug("Sending route info");
                break;

            case ClientConstants.TYPE_QUIT:
                this.quitFlag = true;
                break;

            case ClientConstants.TYPE_WHO:
                break;
        }
    }

    /**
     * Create a new identity.
     *
     * @param identity - New identity received from the client.
     * @throws IOException
     * @throws ParseException
     */
    public void createNewIdentity(String identity) throws ServerException, IOException, ParseException, InterruptedException {
        JSONObject response;

        // Verify identity.
        boolean isAvailable = Consensus.verifyUniqueValue(identity, ChatServerConstants.ServerConstants.IDENTITY);
        logger.debug("New identity availability: " + isAvailable);
        if (!isAvailable) {
            response = buildApprovedJSON(ClientConstants.TYPE_CREATE_ID, ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Add identity to server state.
        ServerState.getServerState().addIdentity(identity);
        // Broadcast identity to other servers.
        //Messaging.broadcastClients(new JSONObject());
        // Send appropriate response.
        response = buildApprovedJSON(ClientConstants.TYPE_CREATE_ID, ClientConstants.TRUE);
        Messaging.respond(response, this.clientSocket);
        // Get mainHall room.
        Room mainHall = ServerState.getServerState().getMainHall();
        // Add client to main hall
        changeRoom(identity, mainHall);
    }

    private void createNewRoom(String room) throws ServerException, IOException, ParseException, InterruptedException {
        JSONObject response;

        // Verify room id.
        boolean isAvailable = Consensus.verifyUniqueValue(room, ChatServerConstants.ServerConstants.ROOM_ID);
        logger.debug("New room id availability: " + isAvailable);
        if (!isAvailable) {
            response = buildApprovedJSON(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        String serverId = ServerState.getServerState().getServerId();
        // Add room to server state.
        ServerState.getServerState().addRoomToMap(new Room(serverId, room));
        // Broadcast room id to other servers.
        // Send appropriate response.
        response = buildApprovedJSON(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.TRUE);
        Messaging.respond(response, this.clientSocket);
    }

    private JSONObject buildApprovedJSON(String type, String approved) {
        HashMap<String, String> response = new HashMap<>();
        response.put(ClientConstants.TYPE, type);
        response.put(ClientConstants.APPROVED, approved);
        return new JSONObject(response);
    }

    private JSONObject buildRoomChangeJSON(String identity, String former, String roomId) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ClientConstants.TYPE, ClientConstants.CHANGE_ROOM);
        request.put(ClientConstants.IDENTITY, identity);
        request.put(ClientConstants.FORMER_ROOM, former);
        request.put(ClientConstants.ROOM_ID, roomId);
        return new JSONObject(request);
    }

    private void changeRoom(String identity, Room room) throws IOException {

        logger.debug("Changing client room to: " + room.getRoomId());
        // TODO: Add client to room's client list upon receiving joinroom request.
        room.addClientIdentity(identity);

        String former;
        if (room.isMainHall()) {
            former = "";
        } else {
            former = "Find previous room of the identity";
        }
        JSONObject roomChangeRequest = buildRoomChangeJSON(identity, former, room.getRoomId());
        Messaging.respond(roomChangeRequest, clientSocket);
    }


}
