package Server;

import Consensus.Leader;
import Constants.ChatServerConstants;
import Messaging.Messaging;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class Framework {

    public static JSONObject createNewIdentity(String identity) throws IOException, ParseException {
        //TODO: Implement new identity logic.

        // Verify identity.
        boolean isAvailable = verifyIdentity(identity);
        // Create identity and move user to mainHall.
        // Broadcast identity to other servers.
        // Send appropriate response.
        JSONObject response;
        response = new JSONObject();
        response.put(ChatServerConstants.ClientConstants.TYPE, ChatServerConstants.ClientConstants.TYPE_CREATE_ID);
        response.put(ChatServerConstants.ClientConstants.APPROVED, ChatServerConstants.ClientConstants.TRUE);
        return response;
    }

    public static boolean verifyIdentity(String identity) throws IOException, ParseException {
        // Get the current Leader.
        Leader leader = ServerState.getServerState().getCurrentLeader();
        if(ServerState.getServerState().getServerId().equals(leader.getId())){
            // Ask from every server.
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ASK", "test");
            Messaging.askServers(jsonObject);
            return !ServerState.getServerState().getIdentityList().contains(identity);
        } else {
            // Contact leader.
        }
        // Ask for identity availability.
        return true;
    }
}
