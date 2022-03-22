package ClientHandler;

import Consensus.Consensus;
import Constants.ChatServerConstants.ClientConstants;
import Constants.ChatServerConstants.ServerConstants;
import Exception.ServerException;
import Server.Room;
import Server.Server;
import Server.ServerState;
import Utilities.Messaging;
import Utilities.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class ClientHandler extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    final Object lock;
    private final Socket clientSocket;
    private boolean quitFlag;
    private String currentIdentity;
    private String currentRoom = "";

    // movejoin related attributes.
    private boolean moveJoinFlag = false;
    private String moveJoinRoom = "";


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

            while (!quitFlag && scanner.hasNextLine()) {
                String line = scanner.nextLine();
                logger.debug("Received from client: " + line);
                resolveClientRequest(Messaging.jsonParseRequest(line));
            }
            // Client quit or got disconnected. Doesn't execute if client moved to another server.
            if (!moveJoinFlag) {
                removeClientFromServer();
            } else {
                // Remove client from room if moved to another server.
                removeClientMoveJoin();
            }

        } catch (IOException | ParseException | ServerException | InterruptedException e) {
            logger.debug(e);
        }
    }

    /**
     * Resolve a given request.
     *
     * @param jsonPayload -  Request as a JSONObject.
     */
    private void resolveClientRequest(JSONObject jsonPayload) throws ServerException, IOException, ParseException, InterruptedException {
        String type = (String) jsonPayload.get("type");


        switch (type) {
            case ClientConstants.TYPE_CREATE_ID:
                String identity = (String) jsonPayload.get(ClientConstants.IDENTITY);
                logger.debug("Creating new identity: " + identity);
                createNewIdentity(identity);
                break;

            case ClientConstants.TYPE_CREATE_ROOM:
                String roomId = Util.getRoomId(jsonPayload);
                logger.debug("Request received to create new room: " + roomId);
                createNewRoom(roomId);
                break;

            case ClientConstants.TYPE_DELETE_ROOM:
                String delRoomId = Util.getRoomId(jsonPayload);
                logger.debug("Request received to delete room: " + delRoomId);
                deleteRoom(delRoomId);
                break;

            case ClientConstants.TYPE_JOIN_ROOM:
                String joinRoomId = Util.getRoomId(jsonPayload);
                logger.debug("Request received to join room: " + joinRoomId);
                joinRoom(joinRoomId);
                break;

            case ClientConstants.TYPE_LIST:
                logger.info("Sending " + currentIdentity + " room list in the system.");
                sendRoomIdsInSystem();
                break;

            case ClientConstants.TYPE_MESSAGE:
                // Broadcast message received by the client.
                broadcastMessage(jsonPayload);
                break;

            case ClientConstants.TYPE_MOVE_JOIN:
                logger.debug("Handling movejoin.");
                handleMoveJoin(jsonPayload);
                break;

            case ClientConstants.TYPE_QUIT:
                logger.info("Client sent quit request.");
                this.quitFlag = true;
                break;

            case ClientConstants.TYPE_WHO:
                logger.info("Sending users in the room " + currentRoom + " to " + currentIdentity);
                sendIdentitiesInRoom();
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
    private void createNewIdentity(String identity) throws ServerException, IOException, ParseException, InterruptedException {
        JSONObject response;

        // Send false if identity doesn't meet the preferred criteria.
        if (Util.meetsCriteria(identity)) {
            logger.info("Identity creation failed. Identity doesn't meet the required criteria.");
            response = Util.buildApprovedJSONId(ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }

        // Verify identity.
        boolean isAvailable = Consensus.getConsensus().verifyUniqueValue(identity, ServerConstants.IDENTITY);
        logger.debug("New identity availability: " + isAvailable);
        if (!isAvailable) {
            response = Util.buildApprovedJSONId(ClientConstants.FALSE);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Add identity to server state.
        ServerState.getServerState().addIdentity(identity);
        // Send the current identity for the client handler.
        currentIdentity = identity;
        // Send appropriate response back to client.
        response = Util.buildApprovedJSONId(ClientConstants.TRUE);
        Messaging.respond(response, this.clientSocket);
        logger.info("Identity: " + identity + " created.");
        // Get mainHall room from ServerState.
        Room mainHall = ServerState.getServerState().getMainHall();
        // Add client to the main hall.
        changeRoom(mainHall);
        // Inform servers about new identity.
        Util.informServersIdentity(ServerConstants.KIND_INFORM_NEW_IDENTITY, identity);
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
        if (Util.meetsCriteria(roomId)) {
            logger.info("Room creation failed. Room Id doesn't meet the required criteria.");
            response = Util.buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }

        // Verify room id.
        boolean isAvailable = Consensus.getConsensus().verifyUniqueValue(roomId, ServerConstants.ROOM_ID);
        logger.debug("New room id availability: " + isAvailable);
        if (!isAvailable) {
            response = Util.buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Get current server id.
        String serverId = ServerState.getServerState().getServerId();
        // Add room to server state.
        ServerState.getServerState().addRoomToMap(new Room(serverId, roomId, currentIdentity));
        // Set current room of the client handler.
        // Send appropriate response back to client.
        response = Util.buildApprovedJSONRoom(ClientConstants.TYPE_CREATE_ROOM, ClientConstants.TRUE, roomId);
        Messaging.respond(response, this.clientSocket);
        logger.info("Room: " + roomId + " created.");

        // Change room of the client.
        changeRoom(ServerState.getServerState().getRoom(roomId));

        // Inform servers about new room.
        Util.informServersRoom(ServerConstants.KIND_INFORM_NEW_ROOM, roomId, currentIdentity);
    }

    /**
     * Create new chat room.
     *
     * @param roomId - Room id sent by the client.
     */
    private void deleteRoom(String roomId) {
        JSONObject response;

        // Get room from server state.
        Room room = ServerState.getServerState().getRoom(roomId);
        if (Util.isMainHall(roomId) || room == null || !currentIdentity.equals(room.getOwner()) || !currentRoom.equals(roomId)) {
            logger.info("Unable to delete room: " + roomId);
            response = Util.buildApprovedJSONRoom(ClientConstants.TYPE_DELETE_ROOM, ClientConstants.FALSE, roomId);
            Messaging.respond(response, this.clientSocket);
            return;
        }
        // Get clients in the room.
        Collection<ClientHandler> roomClients = ServerState.getServerState().getClientsInRoom(roomId);

        // Get mainHall from the ServerState.
        Room mainHall = ServerState.getServerState().getMainHall();
        Collection<ClientHandler> mainHallClients = mainHall.getClientIdentityList();
        // Duplicate client list of main hall.
        Collection<ClientHandler> tempRoomClients = new ArrayList<>(mainHallClients);

        // Move all the client in the room to main hall.
        for (ClientHandler client : roomClients) {
            String prevRoom = client.getCurrentRoom();
            mainHall.addClient(client);
            client.setCurrentRoom(mainHall.getRoomId());
            // Send room change request to client.
            JSONObject roomChangeRequest = Util.buildRoomChangeJSON(
                    client.getCurrentIdentity(), room.getRoomId(), mainHall.getRoomId());
            Messaging.respond(roomChangeRequest, client.getClientSocket());
            // Inform MainHall members about room change of clients.
            broadcastClientChangeRoom(tempRoomClients, client.getCurrentIdentity(), prevRoom, mainHall.getRoomId());

        }
        // Update mainHall in ServerState.
        ServerState.getServerState().updateRoom(mainHall);

        // Set current room.
        currentRoom = mainHall.getRoomId();

        // Remove the room from ServerState.
        ServerState.getServerState().removeRoom(room);
        // Send appropriate response back to client.
        response = Util.buildApprovedJSONRoom(ClientConstants.TYPE_DELETE_ROOM, ClientConstants.TRUE, roomId);
        Messaging.respond(response, this.clientSocket);

        logger.info("Room: " + roomId + " deleted.");
        // Inform servers about delete room.
        Util.informServersRoom(ServerConstants.KIND_INFORM_DELETE_ROOM, roomId, currentIdentity);
    }

    /**
     * Join to a chat room.
     *
     * @param roomId - Room id.
     */
    private void joinRoom(String roomId) {
        Room room = ServerState.getServerState().getRoom(roomId);

        if (room == null) {
            logger.info("Tried joining " + roomId + " room. Room doesn't exist.");
            JSONObject roomChangeRequest = Util.buildRoomChangeJSON(currentIdentity, roomId, roomId);
            Messaging.respond(roomChangeRequest, clientSocket);
            return;
        }

        if (room.getServerId().equals(ServerState.getServerState().getServerId())) {
            logger.debug("Room is hosted in the current server. Moving client: " + currentIdentity + " to room: " + roomId);
            changeRoom(room);
            return;
        }

        // Room is hosted in a different server. Send routing information to client.
        String serverId = room.getServerId();
        logger.info("Requested room is hosted in the server: " + serverId);
        logger.info("Sending route request to client.");
        Server server = ServerState.getServerState().getServerFromId(serverId);
        sendRoutingRequest(roomId, server);
        moveJoinFlag = true;
        moveJoinRoom = roomId;
    }

    /**
     * Send routing request back to client if the room is not in the same server.
     *
     * @param roomId - Room id.
     * @param server - Server which hosts the room.
     */
    private void sendRoutingRequest(String roomId, Server server) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ClientConstants.TYPE, ClientConstants.ROUTE);
        request.put(ClientConstants.HOST, server.getAddress());
        request.put(ClientConstants.PORT, String.valueOf(server.getClientPort()));
        request.put(ClientConstants.ROOM_ID, roomId);
        JSONObject routeRequest = new JSONObject(request);
        Messaging.respond(routeRequest, clientSocket);
    }

    /**
     * Change the room of an identity to a given room.
     *
     * @param room - Room to be assigned to.
     */
    private void changeRoom(Room room) {

        logger.info("Moving client into " + room.getRoomId());
        // Remove client from previous room.
        logger.debug("Current room: " + currentRoom);
        if (!currentRoom.equals("")) {
            Room prevRoom = ServerState.getServerState().getRoom(currentRoom);
            if (prevRoom != null) {
                prevRoom.removeClient(this);
                ServerState.getServerState().updateRoom(prevRoom);
            }
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
     */
    private void informClientChangeRoom(Collection<ClientHandler> clients, String roomId) {
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equals(currentRoom) || client.getCurrentRoom().equals(roomId)) {
                logger.debug("Informing about room change of " + currentIdentity + " to " + client.currentIdentity);
                JSONObject roomChangeRequest = Util.buildRoomChangeJSON(currentIdentity, currentRoom, roomId);
                Messaging.respond(roomChangeRequest, client.getClientSocket());
            }
        }
    }

    /**
     * Inform a set of clients.
     *
     * @param clients Collection clients.
     * @param roomId  New joining room id.
     */
    private void broadcastClientChangeRoom(Collection<ClientHandler> clients, String clientIdentity, String prevRoom, String roomId) {
        for (ClientHandler client : clients) {
            if (client.getCurrentRoom().equals(roomId)) {
                logger.debug("Informing about room change of " + clientIdentity + " to " + client.getCurrentIdentity());
                JSONObject roomChangeRequest = Util.buildRoomChangeJSON(clientIdentity, prevRoom, roomId);
                Messaging.respond(roomChangeRequest, client.getClientSocket());
            }
        }
    }

    /**
     * Broadcast a received message to all the clients in the connected room.
     *
     * @param jsonPayload
     */
    private void broadcastMessage(JSONObject jsonPayload) {
        String message = (String) jsonPayload.get(ClientConstants.CONTENT);
        // Only broadcast messages that contains text.
        if (message != null && (!message.isBlank())) {
            logger.debug("Message '" + message + "' received from client " + currentIdentity);
            // Create message broadcast object
            HashMap<String, String> messageBroadcast = new HashMap<>();
            messageBroadcast.put(ClientConstants.TYPE, ClientConstants.TYPE_MESSAGE);
            messageBroadcast.put(ClientConstants.IDENTITY, currentIdentity);
            messageBroadcast.put(ClientConstants.CONTENT, message);

            // Broadcast message to client in the room.
            Room chatRoom = ServerState.getServerState().getRoom(currentRoom);
            Collection<ClientHandler> clients = chatRoom.getClientIdentityList();
            for (ClientHandler client : clients) {
                // Check for self object.
                if (client.getId() != this.getId()) {
                    Messaging.respond(new JSONObject(messageBroadcast), client.getClientSocket());
                }
            }
        }
    }

    /**
     * Send room ids in the system.
     */
    private void sendRoomIdsInSystem() {
        ArrayList<String> roomIdList = new ArrayList<>();
        Enumeration<String> roomList = ServerState.getServerState().getRoomsIds();
        while (roomList.hasMoreElements()) {
            roomIdList.add(roomList.nextElement());
        }

        HashMap<String, Object> response = new HashMap<>();
        response.put(ClientConstants.TYPE, ClientConstants.ROOM_LIST);
        response.put(ClientConstants.ROOMS, roomIdList);
        Messaging.respond(new JSONObject(response), clientSocket);
    }

    /**
     * Send identities in the current chat room back to client.
     */
    private void sendIdentitiesInRoom() {
        // Get room and its clients from the ServerState.
        Room room = ServerState.getServerState().getRoom(currentRoom);
        Collection<ClientHandler> clients = room.getClientIdentityList();
        // Add client identities into arraylist.
        ArrayList<String> identityList = new ArrayList<>();
        for (ClientHandler client : clients) {
            identityList.add(client.getCurrentIdentity());
        }
        // Send response back to client.
        HashMap<String, Object> response = new HashMap<>();
        response.put(ClientConstants.TYPE, ClientConstants.ROOM_CONTENT);
        response.put(ClientConstants.ROOM_ID, currentRoom);
        response.put(ServerConstants.ROOM_OWNER, room.getOwner());
        response.put(ClientConstants.IDENTITIES, identityList);
        Messaging.respond(new JSONObject(response), clientSocket);
    }

    /**
     * Remove client from current room and client handler from server state.
     * Delete any rooms that client is owner of.
     *
     * @throws IOException
     */
    private void removeClientFromServer() throws IOException {
        logger.info("Client " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " disconnecting.");
        if (currentIdentity != null) {
            logger.debug("Lost connection to: " + currentIdentity);

            // Inform clients in the current room about quitting.
            Collection<ClientHandler> clients = ServerState.getServerState().getClientsInRoom(currentRoom);
            if (clients != null) {
                informClientChangeRoom(clients, "");
            }

            // Remove client from server state.
            logger.debug("Removing identity and client handler from ServerState");
            ServerState.getServerState().deleteIdentity(currentIdentity);
            ServerState.getServerState().removeClientHandler(this);

            // Informing servers about deleted identity.
            Util.informServersIdentity(ServerConstants.KIND_INFORM_DELETE_IDENTITY, currentIdentity);

            // Remove client information from current room.
            Room room = ServerState.getServerState().getRoom(currentRoom);
            logger.debug("Current room:" + currentRoom);
            if (room.getOwner().equals(currentIdentity)) {
                logger.debug("Deleting room: " + currentRoom + " since it is owned by " + currentIdentity);
                deleteRoom(room.getRoomId());
            } else {
                logger.debug("Removing identity from room: " + currentRoom);
                room.removeClient(this);
                ServerState.getServerState().updateRoom(room);
            }
        }
    }

    /**
     * Handle movejoin.
     *
     * @param jsonPayload - JSON payload.
     */
    private void handleMoveJoin(JSONObject jsonPayload) {
        String mjIdentity = (String) jsonPayload.get(ClientConstants.IDENTITY);
        String mjFormerRoom = (String) jsonPayload.get(ClientConstants.FORMER_ROOM);
        String mjRoom = (String) jsonPayload.get(ClientConstants.ROOM_ID);
        logger.debug("Move join request received for client: " + mjIdentity + ". Former room: " + mjFormerRoom);
        logger.debug("Joining new room: " + mjRoom);
        // Set values for new client.
        currentIdentity = mjIdentity;
        currentRoom = mjFormerRoom;
        // Get room from server state.
        Room room = ServerState.getServerState().getRoom(mjRoom);
        // Send serverchange confirmation.
        informServerChange();
        if (room == null) {
            // Add client to main hall.
            Room mainHall = ServerState.getServerState().getMainHall();
            logger.info("The room requested via 'movejoin' is not available. Placing client in the " + mainHall.getRoomId());
            changeRoom(mainHall);
        } else {
            // Add client to the room.
            logger.info("Client connected to room: " + mjRoom + " via 'movejoin' from room: " + mjFormerRoom);
            changeRoom(room);
        }
    }

    /**
     * Confirm about server change.
     */
    private void informServerChange() {
        logger.info("Approved server change confirmation of client: " + currentIdentity);
        HashMap<String, String> request = new HashMap<>();
        request.put(ClientConstants.TYPE, ClientConstants.SERVER_CHANGE);
        request.put(ClientConstants.APPROVED, ClientConstants.TRUE);
        request.put(ClientConstants.SERVER_ID, ServerState.getServerState().getServerId());
        Messaging.respond(new JSONObject(request), this.clientSocket);
    }

    /**
     * Handle room change when client movejoin.
     */
    private void removeClientMoveJoin() {
        if (!moveJoinRoom.equals("")) {
            // Remove client from current room.
            logger.debug("Client moved from room: " + currentRoom + " to another server.");
            Room prevRoom = ServerState.getServerState().getRoom(currentRoom);
            if (prevRoom != null) {
                prevRoom.removeClient(this);
                ServerState.getServerState().updateRoom(prevRoom);
                // Inform about client movejoin.
                Collection<ClientHandler> connectedClients = prevRoom.getClientIdentityList();
                logger.debug("Broadcasting to connected clients in the room: " + moveJoinRoom);
                informClientChangeRoom(connectedClients, moveJoinRoom);
            }
        }
    }

    /**
     * Force the client to quit when server is in a small partition.
     */
    public void forceQuitClient() {
        if (ServerState.getServerState().isSmallPartitionFormed()) {
            quitFlag = true;
            JSONObject roomChangeRequest = Util.buildRoomChangeJSON(currentIdentity, currentRoom, "");
            Messaging.respond(roomChangeRequest, clientSocket);
        }
    }

    public String getCurrentIdentity() {
        return currentIdentity;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
