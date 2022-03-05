package Consensus;

import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import Constants.ChatServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;


public class LeaderElection {
    private static final String thisServerId = ServerState.getServerState().getServerId();
    private static final Collection<Server> allServers = ServerState.getServerState().getServers();
    private static final Logger logger = LogManager.getLogger(LeaderElection.class);
    public static void startElection() {
        //Build the request object
        Collection<JSONObject> replies = sendElectionStartMessage();
        if (replies.isEmpty()){
            // I am the leader
            // Tell the world
        }else{

        }

    }

    /**
     * Bully messages have the same format. type, kind, and serverId
     * TYPE is always Bully. Other kind and serverId changes.
     * @param kind Kind of the message: ELECTION, OK, ELECTED, COORDINATOR
     * @param serverId ServerId of the sender or the elected leader, depending on the message.
     * @return JSONObject
     */
    private static JSONObject buildElectionJSON(String kind, String serverId){
        HashMap<String, String> message = new HashMap<>();
        message.put(ChatServerConstants.BullyConstants.TYPE, ChatServerConstants.BullyConstants.TYPE_BULLY);
        message.put(ChatServerConstants.BullyConstants.KIND, kind);
        message.put(ChatServerConstants.BullyConstants.SERVER_ID, serverId);
        return new JSONObject(message);
    }

    private static Collection<JSONObject> sendElectionStartMessage(){
        JSONObject message = buildElectionJSON(ChatServerConstants.BullyConstants.KIND_ELECTION, thisServerId);
        return Messaging.askServers(message, ServerState.getServerState().getServers()).values();
    }

    /**
     * Reply to an ELECTION message
     * Only replies OK if this server's server ID is larger than election starter's server ID
     * @param electionStarterId Server ID of the election starter
     */
    private static void replyOK(String electionStarterId){
        JSONObject message = buildElectionJSON(ChatServerConstants.BullyConstants.KIND_OK, thisServerId);
        for(Server s: ServerState.getServerState().getServers()){
            if (s.getId().equals(electionStarterId) &&
                    Integer.parseInt(thisServerId) > Integer.parseInt(electionStarterId)){
                logger.debug("Sending OK message to: "+electionStarterId+" from: "+thisServerId);
                Messaging.askServers(new JSONObject(message), List.of(s));
                return;
            }
        }
        logger.error("This line should never reached; OK message sent by a non existing server");
    }

    /**
     * Send the "ELECTED" message to the winner of the election (Sever with the highest id)
     * @param repliedServers servers that replied OK
     */
    private static void sendElectedMessage(Collection<Server> repliedServers){
        HashMap<String, String> electedMessage = new HashMap<>();
        electedMessage.put(ChatServerConstants.BullyConstants.TYPE, ChatServerConstants.BullyConstants.TYPE_BULLY);
        electedMessage.put(ChatServerConstants.BullyConstants.KIND, ChatServerConstants.BullyConstants.KIND_COORDINATOR);
        electedMessage.put(ChatServerConstants.BullyConstants.SERVER_ID, ServerState.getServerState().getServerId());
        //Choose the highest id from the replied servers
//        List<Server> lowerIdServers = ServerState.getServerState().getServers().stream().filter((Server s)-> Integer.parseInt(s.getId())<Integer.parseInt(ServerState.getServerState().getServerId())).collect(Collectors.toList());
// todo complete
//        Messaging.askServers(new JSONObject(electedMessage), null);
    }


    private static void announceToTheWorld(){
        JSONObject message = buildElectionJSON(ChatServerConstants.BullyConstants.KIND_COORDINATOR, thisServerId);
        Messaging.askServers(new JSONObject(message), allServers);
    }
}
