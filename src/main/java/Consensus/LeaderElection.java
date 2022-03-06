package Consensus;

import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import Constants.ChatServerConstants.ServerConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class LeaderElection {
    private static final Logger logger = LogManager.getLogger(LeaderElection.class);
    private static boolean electionStarter = false;
    private static boolean electionFlag = false;

    //No objects from LeaderElection class
    private LeaderElection(){

    }

    public static void startElection() {
        electionStarter = true;
        Collection<JSONObject> replies = sendElectionStartMessage();
        if (replies.isEmpty()) {
            // I am the leader
            // Tell the world
        } else {

        }

    }

    private static String getThisServerId(){
        return ServerState.getServerState().getServerId();
    }

    private static Collection<Server> getAllServers(){
        return ServerState.getServerState().getServers();
    }

    /**
     * Bully messages have the same format. type, kind, and serverId
     * TYPE is always Bully. Other kind and serverId changes.
     *
     * @param kind     Kind of the message: ELECTION | OK | ELECTED | COORDINATOR
     * @param serverId ServerId of the sender or the elected leader, depending on the message.
     * @return JSONObject
     */
    private static JSONObject buildElectionJSON(String kind, String serverId) {
        HashMap<String, String> message = new HashMap<>();
        message.put(ServerConstants.TYPE, ServerConstants.TYPE_BULLY);
        message.put(ServerConstants.KIND, kind);
        message.put(ServerConstants.SERVER_ID, serverId);
        return new JSONObject(message);
    }

    private static Collection<JSONObject> sendElectionStartMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTION, getThisServerId());
        return Messaging.askServers(message, ServerState.getServerState().getServers()).values();
    }

    /**
     * Reply to an ELECTION message
     * Only replies OK if this server's server ID is larger than election starter's server ID
     *
     * @param electionStarterId Server ID of the election starter
     */
    private static void replyOK(String electionStarterId) {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_OK, getThisServerId());
        for (Server s : ServerState.getServerState().getServers()) {
            if (s.getId().equals(electionStarterId) &&
                    Integer.parseInt(getThisServerId()) > Integer.parseInt(electionStarterId)) {
                logger.debug("Sending OK message to: " + electionStarterId + " from: " + getThisServerId());
                Messaging.askServers(new JSONObject(message), List.of(s));
                return;
            }
        }
        logger.error("This line should never reached; OK message sent by a non existing server");
    }

    /**
     * Send the "ELECTED" message to the winner of the election (Replied sever with the highest id)
     *
     * @param repliedServers servers that replied OK
     */
    private static void sendElectedMessage(Collection<Server> repliedServers) {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Server max = repliedServers.stream().max(new Server.ServerComparator()).stream().findFirst().orElse(null);
        if (max == null) {
            logger.error("No servers have replied");
        } else {
            Messaging.askServers(new JSONObject(message), List.of(max));
        }
    }

    private static void receiveElectedMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Predicate<Server> higherServerPredicate = server -> Integer.parseInt(server.getId()) > Integer.parseInt(getThisServerId());
        Collection<Server> higherServers = getAllServers().stream().filter(higherServerPredicate).collect(Collectors.toList());
        if (!higherServers.isEmpty()) {
            Messaging.askServers(new JSONObject(message), higherServers);
        } else {
            logger.debug("I am the server with the highest server ID: "+getThisServerId());
        }
    }


    private static void announceToTheWorld() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_COORDINATOR, getThisServerId());
        Messaging.askServers(new JSONObject(message), getAllServers());
    }
}
