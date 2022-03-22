package Utilities;

import Constants.ChatServerConstants;
import Server.Server;
import Server.ServerState;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class Util {
    /**
     * Select n number of random servers out of a given collection of servers.
     * Selection happens without replacement.
     *
     * @param servers servers Collection
     * @param n       n
     * @return Collection<Server>
     */
    public static Collection<Server> getRandomServers(Collection<Server> servers, int n) {
        ArrayList<Server> serversList = new ArrayList<>(servers);
        Random random = new Random();
        Collection<Server> randomServers = new ArrayList<>();
        //select servers randomly
        for (int i = 0; i < n; i++) {
            randomServers.add(serversList.get(random.nextInt(serversList.size())));
        }
        return randomServers;
    }

    /**
     * Inform servers about room creation/ deletion.
     *
     * @param kind   - Kind.
     * @param roomId - Room Id.
     * @param owner
     */
    public static void informServersRoom(String kind, String roomId, String owner) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ServerConstants.TYPE, ChatServerConstants.ServerConstants.TYPE_GOSSIP);
        request.put(ChatServerConstants.ServerConstants.KIND, kind);
        request.put(ChatServerConstants.ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ChatServerConstants.ServerConstants.ROOM_ID, roomId);
        request.put(ChatServerConstants.ServerConstants.ROOM_OWNER, owner);
        Collection<Server> servers = ServerState.getServerState().getServers();
        Messaging.sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Create a JSON object to inform about room change to clients.
     *
     * @param identity - Identity of the client.
     * @param former   - Previous room client was in.
     * @param roomId   - New room id.
     * @return - JSON object.
     */
    public static JSONObject buildRoomChangeJSON(String identity, String former, String roomId) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ClientConstants.TYPE, ChatServerConstants.ClientConstants.CHANGE_ROOM);
        request.put(ChatServerConstants.ClientConstants.IDENTITY, identity);
        request.put(ChatServerConstants.ClientConstants.FORMER_ROOM, former);
        request.put(ChatServerConstants.ClientConstants.ROOM_ID, roomId);
        return new JSONObject(request);
    }

    /**
     * Inform servers about identity creation deletion.
     *
     * @param kind     - Kind.
     * @param identity - Identity.
     */
    public static void informServersIdentity(String kind, String identity) {
        HashMap<String, String> request = new HashMap<>();
        request.put(ChatServerConstants.ServerConstants.TYPE, ChatServerConstants.ServerConstants.TYPE_GOSSIP);
        request.put(ChatServerConstants.ServerConstants.KIND, kind);
        request.put(ChatServerConstants.ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());
        request.put(ChatServerConstants.ServerConstants.IDENTITY, identity);
        Collection<Server> servers = ServerState.getServerState().getServers();
        Messaging.sendAndForget(new JSONObject(request), servers);
    }

    /**
     * Check whether a given room is the main hall.
     *
     * @param roomId - Room id.
     * @return - Boolean value.
     */
    public static boolean isMainHall(String roomId) {
        return roomId.equals(ServerState.getServerState().getMainHall().getRoomId());
    }

    /**
     * Create a response JSON object for create identity scenario.
     *
     * @param approved - Boolean value indicating whether the identity was approved or not.
     * @return -  Response JSON object.
     */
    public static JSONObject buildApprovedJSONId(String approved) {
        HashMap<String, String> response = new HashMap<>();
        response.put(ChatServerConstants.ClientConstants.TYPE, ChatServerConstants.ClientConstants.TYPE_CREATE_ID);
        response.put(ChatServerConstants.ClientConstants.APPROVED, approved);
        return new JSONObject(response);
    }

    /**
     * Create a response JSON object for create room scenario.
     *
     * @param type     - Approved type.
     * @param approved - Boolean value indicating whether the room id was approved or not.
     * @return -  Response JSON object.
     */
    public static JSONObject buildApprovedJSONRoom(String type, String approved, String roomId) {
        HashMap<String, String> response = new HashMap<>();
        response.put(ChatServerConstants.ClientConstants.TYPE, type);
        response.put(ChatServerConstants.ClientConstants.APPROVED, approved);
        response.put(ChatServerConstants.ClientConstants.ROOM_ID, roomId);
        return new JSONObject(response);
    }

    /**
     * Extract room id value from request.
     *
     * @param jsonPayload - JSON payload.
     * @return - Room id extracted.
     */
    public static String getRoomId(JSONObject jsonPayload) {
        return (String) jsonPayload.get(ChatServerConstants.ClientConstants.ROOM_ID);
    }

    /**
     * Check whether a given string adheres to the criteria.
     *
     * @param value - Identity or Room id.
     * @return - Boolean value.
     */
    public static boolean meetsCriteria(String value) {
        return ((value.length() < 3) || (value.length() > 16) || !Character.isLetter(value.charAt(0)));
    }
}
