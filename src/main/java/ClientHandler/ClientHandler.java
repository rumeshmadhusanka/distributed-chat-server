package ClientHandler;

import Consensus.Consensus;
import Constants.ChatServerConstants.ClientConstants;
import Constants.ChatServerConstants.ServerConstants;
import Exception.ServerException;
import Messaging.Messaging;
import Server.Room;
import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Scanner;


public class ClientHandler extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    final Object lock;
    private final Socket clientSocket;
    private boolean quitFlag;
    private String currentIdentity;
    private String currentRoom = "";


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
                createNewIdentity(identity);
                break;

            case ClientConstants.TYPE_CREATE_ROOM:
                logger.debug("Resolving create new room.");
                String roomId = (String) jsonPayload.get(ClientConstants.ROOM_ID);
                logger.debug("New room: " + roomId);
                createNewRoom(roomId);
                break;

            case ClientConstants.TYPE_DELETE_ROOM:
                logger.debug("Deleting room");
                String delRoomId = (String) jsonPayload.get(ClientConstants.ROOM_ID);
                logger.debug("Delete room: " + delRoomId);
                deleteRoom(delRoomId);
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
     * Check whether a given string adheres to the criteria.
     *
     * @param value - Identity or Room id.
     * @return - Boolean value.
     */
    private boolean meetsCriteria(String value) {
        return ((value.length() < 3) || (value.length() > 16) || !Character.isLetter(value.charAt(0)));
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

        // Send false if identity doesn't meet the preferred criteria.
        if (meetsCriteria(identity)) {
            response = buildApprovedJSONId(ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }

        // Verify identity.
        boolean isAvailable = Consensus.getConsensus().verifyUniqueValue(identity, ServerConstants.IDENTITY);
        logger.debug("New identity availability: " + isAvailable);
        if (!isAvailable) {
            response = buildApprovedJSONId(ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Add identity to server state.
        ServerState.getServerState().addIdentity(identity);
        // Send the current identity for the client handler.
        currentIdentity = identity;
        //TODO: Broadcast identity to other servers.
        //Messaging.broadcastClients(new JSONObject());
        // Send appropriate response back to client.
        response = buildApprovedJSONId(ClientConstants.TRUE);
        Messaging.respond(response, this.clientSocket);
        // Get mainHall room from ServerState.
        Room mainHall = ServerState.getServerState().getMainHall();
        // Add client to the main hall.
        changeRoom(mainHall);

        // Inform servers about new identity.
        informServersIdentity(ServerConstants.KIND_INFORM_NEW_IDENTITY, identity);
    }

    /**
     * Create new chat room.
     *
     * @param roomId - Room id sent by the client.
     * @throws ServerException
     * @throws IOException
     * @throws ParseException
     * @throws InterruptedException
     */
    private void createNewRoom(String roomId) throws ServerException, IOException, ParseException, InterruptedException {
        JSONObject response;

        // Send false if room id doesn't meet the preferred criteria.
        if (meetsCriteria(roomId)) {
            response = buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }

        // Verify room id.
        boolean isAvailable = Consensus.getConsensus().verifyUniqueValue(roomId, ServerConstants.ROOM_ID);
        logger.debug("New room id availability: " + isAvailable);
        if (!isAvailable) {
            response = buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Get current server id.
        String serverId = ServerState.getServerState().getServerId();
        // Add room to server state.
        ServerState.getServerState().addRoomToMap(new Room(serverId, roomId, currentIdentity));
        // Set current room of the client handler.
        // Send appropriate response back to client.
        response = buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.TRUE, roomId);
        Messaging.respond(response, this.clientSocket);

        // Change room of the client.
        changeRoom(ServerState.getServerState().getRoom(roomId));

        // Inform servers about new room.
        informServersRoom(ServerConstants.KIND_INFORM_NEW_ROOM, roomId);
    }

    /**
     * Create new chat room.
     *
     * @param roomId - Room id sent by the client.
     * @throws IOException
     */
    private void deleteRoom(String roomId) throws IOException {
        JSONObject response;

        // Get room from server state.
        Room room = ServerState.getServerState().getRoom(roomId);
        if (isMainHall(roomId) || room == null || !currentIdentity.equals(room.getOwner())) {
            response = buildApprovedJSONRoom(ClientConstants.TYPE_DELETE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Get clients in the room.
        Collection<ClientHandler> roomClients = ServerState.getServerState().getClientsInRoom(roomId);

        // Get mainHall from the ServerState.
        Room mainHall = ServerState.getServerState().getMainHall();
        Collection<ClientHandler> mainHallClients = ServerState.getServerState().getClientsInRoom(mainHall.getRoomId());
        Collection<ClientHandler> tempRoomClients = new ArrayList<>(mainHallClients);

        // Move all the client in the room to main hall.
        for (ClientHandler client : roomClients) {
            mainHall.addClient(client);
            JSONObject roomChangeRequest = buildRoomChangeJSON(
                    client.getCurrentIdentity(), room.getRoomId(), mainHall.getRoomId());
            Messaging.respond(roomChangeRequest, client.getClientSocket());
        }
        // Update mainHall in ServerState.
        ServerState.getServerState().updateRoom(mainHall);

        // Inform clients in main hall.
        informClientChangeRoom(tempRoomClients, mainHall.getRoomId());

        // Remove the room from ServerState.
        ServerState.getServerState().removeRoom(room);
        // Send appropriate response back to client.
        response = buildApprovedJSONRoom(ClientConstants.TYPE_DELETE_ROOM, ClientConstants.TRUE, roomId);
        Messaging.respond(response, this.clientSocket);

        // Inform servers about delete room.
        informServersRoom(ServerConstants.KIND_INFORM_DELETE_ROOM, roomId);
    }

    /**
     * Inform servers about identity creation deletion.
     *
     * @param kind     - Kind.
     * @param identity - Identity.
     */
    private void informServersIdentity(String kind, String identity) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ServerConstants.TYPE, ServerConstants.TYPE_GOSSIP);
        request.put(ServerConstants.KIND, kind);
        request.put(ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ServerConstants.IDENTITY, identity);
        Collection<Server> servers = ServerState.getServerState().getServers();
        Messaging.sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Inform servers about room creation/ deletion.
     *
     * @param kind   - Kind.
     * @param roomId - Room Id.
     */
    private void informServersRoom(String kind, String roomId) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ServerConstants.TYPE, ServerConstants.TYPE_GOSSIP);
        request.put(ServerConstants.KIND, kind);
        request.put(ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ServerConstants.ROOM_ID, roomId);
        request.put(ServerConstants.ROOM_OWNER, currentIdentity);
        Collection<Server> servers = ServerState.getServerState().getServers();
        Messaging.sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Check whether a given room is the main hall.
     *
     * @param roomId - Room id.
     * @return - Boolean value.
     */
    private boolean isMainHall(String roomId) {
        return roomId.equals(ServerState.getServerState().getMainHall().getRoomId());
    }

    /**
     * Create a response JSON object for create identity scenario.
     *
     * @param approved - Boolean value indicating whether the identity was approved or not.
     * @return -  Response JSON object.
     */
    private JSONObject buildApprovedJSONId(String approved) {
        HashMap<String, String> response = new HashMap<>();
        response.put(ClientConstants.TYPE, ClientConstants.TYPE_CREATE_ID);
        response.put(ClientConstants.APPROVED, approved);
        return new JSONObject(response);
    }

    /**
     * Create a response JSON object for create room scenario.
     *
     * @param type     - Approved type.
     * @param approved - Boolean value indicating whether the room id was approved or not.
     * @return -  Response JSON object.
     */
    private JSONObject buildApprovedJSONRoom(String type, String approved, String roomId) {
        HashMap<String, String> response = new HashMap<>();
        response.put(ClientConstants.TYPE, type);
        response.put(ClientConstants.APPROVED, approved);
        response.put(ClientConstants.ROOM_ID, roomId);
        return new JSONObject(response);
    }

    /**
     * Create a JSON object to inform about room change to clients.
     *
     * @param identity - Identity of the client.
     * @param former   - Previous room client was in.
     * @param roomId   - New room id.
     * @return - JSON object.
     */
    private JSONObject buildRoomChangeJSON(String identity, String former, String roomId) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ClientConstants.TYPE, ClientConstants.CHANGE_ROOM);
        request.put(ClientConstants.IDENTITY, identity);
        request.put(ClientConstants.FORMER_ROOM, former);
        request.put(ClientConstants.ROOM_ID, roomId);
        return new JSONObject(request);
    }

    /**
     * Change the room of an identity to a given room.
     *
     * @param room - Room to be assigned to.
     * @throws IOException
     */
    private void changeRoom(Room room) throws IOException {

        logger.debug("Changing client room to: " + room.getRoomId());
        // Remove client from previous room.
        logger.debug("Current room: " + currentRoom);
        if (!currentRoom.equals("")) {
            Room prevRoom = ServerState.getServerState().getRoom(currentRoom);
            prevRoom.removeClient(this);
            ServerState.getServerState().updateRoom(prevRoom);
        }
        // Add client to new room.
        room.addClient(this);
        ServerState.getServerState().updateRoom(room);
        Collection<ClientHandler> connectedClients = ServerState.getServerState().getClientHandlerHashMap().values();
        logger.debug("Broadcasting to connected clients in the room: " + room.getRoomId());
        informClientChangeRoom(connectedClients, room.getRoomId());
        currentRoom = room.getRoomId();
    }

    /**
     * Inform a set of clients.
     *
     * @param clients Collection clients.
     * @param roomId  New joining room id.
     * @throws IOException
     */
    private void informClientChangeRoom(Collection<ClientHandler> clients, String roomId) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equals(this.currentRoom) || client.getCurrentRoom().equals(roomId)) {
                JSONObject roomChangeRequest = buildRoomChangeJSON(currentIdentity, currentRoom, roomId);
                Messaging.respond(roomChangeRequest, client.getClientSocket());
            }
        }
    }

    public String getCurrentIdentity() {
        return currentIdentity;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
