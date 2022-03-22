package Server;

import ClientHandler.ClientHandler;
import Consensus.Consensus;
import Consensus.LeaderElection;
import Constants.ChatServerConstants.ServerConstants;
import Constants.ChatServerConstants.ServerExceptionConstants;
import Exception.ServerException;
import Gossiping.Gossiping;
import Utilities.Messaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;

public class ServerHandler extends Thread {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private final Socket serverSocket;

    public ServerHandler(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            InputStream inputFromClient = serverSocket.getInputStream();
            Scanner serverInputScanner = new Scanner(inputFromClient, String.valueOf(StandardCharsets.UTF_8));
            String line = serverInputScanner.nextLine();
            logger.trace("Received: " + line);
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonPayload = (JSONObject) jsonParser.parse(line);
            resolveServerRequest(jsonPayload);
        } catch (IOException | ParseException | ServerException | InterruptedException | ClassNotFoundException e) {
            logger.debug(e);
        }
    }

    /**
     * Resolve a received json request.
     *
     * @param jsonPayload - Received payload as a JSONObject.
     * @throws IOException
     * @throws ParseException
     * @throws ServerException
     */
    private void resolveServerRequest(JSONObject jsonPayload) throws IOException, ParseException, ServerException, InterruptedException, ClassNotFoundException {
        String type = (String) jsonPayload.get(ServerConstants.TYPE);
        String kind = (String) jsonPayload.get(ServerConstants.KIND);

        switch (type) {
            case ServerConstants.TYPE_CONSENSUS:
                switch (kind) {
                    case ServerConstants.KIND_VERIFY_UNIQUE:
                        logger.info("Consensus: Unique value verification request received.");
                        verifyUnique(jsonPayload);
                        break;
                    case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_IDENTITY:
                        logger.info("Consensus: New identity creation request received to leader.");
                        handleRequestToCreate(jsonPayload, ServerConstants.IDENTITY);
                        break;
                    case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_ROOM:
                        logger.info("Consensus: New room creation request received to leader.");
                        handleRequestToCreate(jsonPayload, ServerConstants.ROOM_ID);
                        break;
                }
                break;
            case ServerConstants.TYPE_GOSSIP:
                switch (kind) {
                    case ServerConstants.KIND_INFORM_NEW_IDENTITY:
                        logger.info("Gossiping: New identity creation information received.");
                        addNewIdentity(jsonPayload);
                        break;

                    case ServerConstants.KIND_INFORM_DELETE_IDENTITY:
                        logger.info("Gossiping: Identity deletion information received.");
                        deleteIdentity(jsonPayload);
                        break;

                    case ServerConstants.KIND_INFORM_NEW_ROOM:
                        logger.info("Gossiping: New room creation information received.");
                        addNewRoom(jsonPayload);
                        break;

                    case ServerConstants.KIND_INFORM_DELETE_ROOM:
                        logger.info("Gossiping: Room deletion information received.");
                        deleteRoom(jsonPayload);
                        break;

                    case ServerConstants.KIND_HEARTBEAT:
                        Gossiping.receiveHeartBeat(jsonPayload);
                        break;
                }
                break;
            case ServerConstants.TYPE_BULLY:
                switch (kind) {
                    case ServerConstants.KIND_ELECTION:
                        // This server received an ELECTION message
                        logger.trace("Received bully to: " + ServerState.getServerState().getServerId() + " by: " + jsonPayload.get(ServerConstants.SERVER_ID));
                        LeaderElection.replyOKorPass(jsonPayload, serverSocket);
                        break;

                    case ServerConstants.KIND_ELECTED:
                        // This server received elected message
                        logger.trace("Received ELECTED to: " + ServerState.getServerState().getServerId() + " by: " + jsonPayload.get(ServerConstants.SERVER_ID));
                        LeaderElection.respondToElectedMessage();
                        break;

                    case ServerConstants.KIND_COORDINATOR:
                        logger.trace("Received COORDINATOR to: " + ServerState.getServerState().getServerId() + " by: " + jsonPayload.get(ServerConstants.SERVER_ID));
                        LeaderElection.receiveCoordinator(jsonPayload);
                        break;
                }
                break;
            case ServerConstants.LEADER_STATE_MERGE:
                logger.info("State Received from the leader.");
                ServerState.getServerState().restoreServerState(jsonPayload);
                break;
        }
    }

    /**
     * Add newly created identity.
     *
     * @param jsonPayload - JSON payload.
     */
    private void addNewIdentity(JSONObject jsonPayload) {
        String identity = (String) jsonPayload.get(ServerConstants.IDENTITY);
        ServerState.getServerState().addIdentity(identity);
    }

    /**
     * Remove delete identity from the list.
     *
     * @param jsonPayload - JSON payload.
     */
    private void deleteIdentity(JSONObject jsonPayload) {
        String identity;
        identity = (String) jsonPayload.get(ServerConstants.IDENTITY);
        ServerState.getServerState().deleteIdentity(identity);
    }

    /**
     * Add newly created room.
     *
     * @param jsonPayload - JSON payload.
     */
    private void addNewRoom(JSONObject jsonPayload) {
        Room newRoom = getRoomFromRequest(jsonPayload);
        ServerState.getServerState().addRoomToMap(newRoom);
    }

    /**
     * Remove deleted room from the hashmap.
     *
     * @param jsonPayload - JSON payload.
     */
    private void deleteRoom(JSONObject jsonPayload) {
        String delRoomId = extractRoomIdFromRequest(jsonPayload);
        Room delRoom = ServerState.getServerState().getRoom(delRoomId);
        // Remove clients from the room, if the room is hosted in this server.
        if (delRoom != null) {
            if (delRoom.getServerId().equals(ServerState.getServerState().getServerId())) {
                logger.info("Removing clients from room before deletion");
                delRoom.removeClientsFromRoom();
            }
            ServerState.getServerState().removeRoom(delRoom);
        }
    }

    /**
     * Extract room from a JSON payload.
     *
     * @param jsonPayload - JSON payload.
     * @return - Room object.
     */
    private Room getRoomFromRequest(JSONObject jsonPayload) {
        String roomId = (String) jsonPayload.get(ServerConstants.ROOM_ID);
        String serverId = (String) jsonPayload.get(ServerConstants.SERVER_ID);
        String owner = (String) jsonPayload.get(ServerConstants.ROOM_OWNER);
        return new Room(serverId, roomId, owner);
    }

    /**
     * Extract room from a JSON payload with correct serverId for deletion purposes.
     *
     * @param jsonPayload - JSON payload.
     * @return - Room object.
     */
    private String extractRoomIdFromRequest(JSONObject jsonPayload) {
        return (String) jsonPayload.get(ServerConstants.ROOM_ID);
    }

    /**
     * Verify whether the given value is unique or not.
     *
     * @param jsonPayload - JSON payload.
     */
    private void verifyUnique(JSONObject jsonPayload) {
        String value;
        String valueType;
        boolean isAvailable;
        // Find the verifyUnique type and query ServerState.
        if (jsonPayload.containsKey(ServerConstants.IDENTITY)) {
            value = String.valueOf(jsonPayload.get(ServerConstants.IDENTITY));
            valueType = ServerConstants.IDENTITY;
            isAvailable = !ServerState.getServerState().hasIdentity(value);
        } else {
            value = String.valueOf(jsonPayload.get(ServerConstants.ROOM_ID));
            valueType = ServerConstants.ROOM_ID;
            isAvailable = !ServerState.getServerState().hasRoomId(value);
        }

        HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
        responseMap.put(ServerConstants.KIND, ServerConstants.KIND_VERIFY_UNIQUE);
        responseMap.put(valueType, value);
        responseMap.put(ServerConstants.UNIQUE, String.valueOf(isAvailable));
        Messaging.respond(new JSONObject(responseMap), serverSocket);
    }

    /**
     * Handle request to create.
     *
     * @param jsonPayload - JSON payload.
     * @param type        - Type of the request.
     * @throws ServerException
     * @throws IOException
     * @throws ParseException
     * @throws InterruptedException
     */
    private void handleRequestToCreate(JSONObject jsonPayload, String type) throws ServerException, IOException, ParseException, InterruptedException {
        boolean isAvailable;
        JSONObject response = null;
        switch (type) {
            case ServerConstants.IDENTITY:
                String identity = String.valueOf(jsonPayload.get(ServerConstants.IDENTITY));
                isAvailable = Consensus.getConsensus().verifyUniqueValue(identity, ServerConstants.IDENTITY);
                response = createRequestKindJSON(isAvailable, identity,
                        ServerConstants.KIND_REQUEST_TO_CREATE_NEW_IDENTITY);
                break;
            case ServerConstants.ROOM_ID:
                String roomId = String.valueOf(jsonPayload.get(ServerConstants.ROOM_ID));
                isAvailable = Consensus.getConsensus().verifyUniqueValue(roomId, ServerConstants.ROOM_ID);
                response = createRequestKindJSON(isAvailable, roomId,
                        ServerConstants.KIND_REQUEST_TO_CREATE_NEW_ROOM);
        }
        if (response != null) {
            Messaging.respond(response, serverSocket);
        } else {
            throw new ServerException(
                    ServerExceptionConstants.INTERNAL_SERVER_ERROR_MSG,
                    ServerExceptionConstants.INTERNAL_SERVER_ERROR_CODE);
        }
    }

    /**
     * Create a JSON object.
     *
     * @param isAvailable - Indicating whether the value is available.
     * @param value       - Value.
     * @param type        - Type of the value.
     * @return - JSONObject.
     */
    private JSONObject createRequestKindJSON(boolean isAvailable, String value, String type) {
        HashMap<String, String> responseMap = new HashMap<>();
        responseMap.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
        responseMap.put(ServerConstants.SUCCESS, String.valueOf(isAvailable));
        switch (type) {
            case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_IDENTITY:
                responseMap.put(ServerConstants.KIND, ServerConstants.KIND_REPLY_TO_CREATE_NEW_IDENTITY);
                responseMap.put(ServerConstants.IDENTITY, value);
                break;
            case ServerConstants.KIND_REQUEST_TO_CREATE_NEW_ROOM:
                responseMap.put(ServerConstants.KIND, ServerConstants.KIND_REPLY_TO_CREATE_NEW_ROOM);
                responseMap.put(ServerConstants.ROOM_ID, value);
        }
        return new JSONObject(responseMap);
    }
}
