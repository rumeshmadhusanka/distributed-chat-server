package Utilities;

import Constants.ChatServerConstants;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class Util {

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
}
