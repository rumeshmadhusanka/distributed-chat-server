package Server;

import Consensus.Leader;
import Constants.ChatServerConstants.ServerConstants;
import Constants.ChatServerConstants.ClientConstants;
import Messaging.Messaging;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class Framework {

    public static JSONObject createNewIdentity(String identity) throws IOException, ParseException {
        //TODO: Implement new identity logic.

        // Verify identity.
        boolean isAvailable = askServers(identity, ServerConstants.IDENTITY);
        // Create identity and move user to mainHall.
        // Broadcast identity to other servers.
        // Send appropriate response.
        JSONObject response;
        response = new JSONObject();
        response.put(ClientConstants.TYPE, ClientConstants.TYPE_CREATE_ID);
        response.put(ClientConstants.APPROVED, ClientConstants.TRUE);
        return response;
    }

    public static boolean askServers(String value, String askType) throws IOException, ParseException {
        // Get the current Leader.
        Leader leader = ServerState.getServerState().getCurrentLeader();
        // Check if leader is this server.
        if(ServerState.getServerState().getServerId().equals(leader.getId())){
            // Get identity verification from every server.
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
            switch (askType){
                case ServerConstants.IDENTITY:
                    jsonObject.put(ServerConstants.KIND, ServerConstants.KIND_ASK_IDENTITY);
                    jsonObject.put(ServerConstants.IDENTITY, value);
                    break;
                case ServerConstants.ROOM:
                    jsonObject.put(ServerConstants.KIND, ServerConstants.KIND_ASK_ROOM);
                    jsonObject.put(ServerConstants.ROOM, value);
            }
            Collection servers = ServerState.getServerState().getServers();
            HashMap<String, JSONObject> responses = Messaging.askServers(jsonObject, servers);

            return !ServerState.getServerState().getIdentityList().contains(value);
        } else {

            // Contact leader.
            // if leader fails, call for election.
        }
        // Ask for identity availability.
        return true;
    }
}
