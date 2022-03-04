package Consensus;

import Constants.ChatServerConstants.ServerConstants;
import Constants.ChatServerConstants.ServerExceptionConstants;
import Exception.ServerException;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class Consensus {

    public static boolean verifyUniqueValue(String value, String askType) throws IOException, ParseException {
        Leader leader = ServerState.getServerState().getCurrentLeader();
        boolean isUnique = true;
        if (ServerState.getServerState().getServerId().equals(leader.getId())) {
            // Get verification from every server.
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
            hashMap.put(ServerConstants.KIND, ServerConstants.KIND_VERIFY_UNIQUE);
            switch (askType) {
                case ServerConstants.IDENTITY:
                    hashMap.put(ServerConstants.IDENTITY, value);
                    break;
                case ServerConstants.ROOM_ID:
                    hashMap.put(ServerConstants.ROOM_ID, value);
            }
            Collection<Server> servers = ServerState.getServerState().getServers();
            HashMap<String, JSONObject> responses = Messaging.askServers(new JSONObject(hashMap), servers);

            for (JSONObject response : responses.values()) {
                if (Boolean.parseBoolean((String) response.get("unique"))) {
                    isUnique = false;
                    break;
                }
            }
        } else {
            try {
                // TODO:Contact leader for verification.

                // Throw an error if connection to leader fails. (Implement this within connection method).
                throw new ServerException(
                        ServerExceptionConstants.LEADER_FAILED_MSG,
                        ServerExceptionConstants.LEADER_FAILED_CODE);

                // Get verification from leader and update isUnique.

            } catch (ServerException err) {
                if (err.getCode().equals(ServerExceptionConstants.LEADER_FAILED_CODE)) {
                    // TODO:Start Election
                }
            }
        }
        return isUnique;
    }

}
