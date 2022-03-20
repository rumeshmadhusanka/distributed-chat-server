package Consensus;

import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LeaderElection {
    private static final Logger logger = LogManager.getLogger(LeaderElection.class);
    public static boolean electionFlag = false; // an election is running
    private static Thread leaderElectionThread = null;

    //No objects from LeaderElection class
    private LeaderElection() {

    }

    public static void startElection() {
        if (electionFlag) {
            logger.debug("An election process is already running");
            return;
        }
        electionFlag = true;
        ServerState.getServerState().setCurrentLeader(null);
        leaderElectionThread = new Thread(() -> {
            ConcurrentHashMap<String, JSONObject> replies = sendElectionStartMessage();
            Collection<String> okReplies = new ArrayList<>();
            for (Map.Entry<String, JSONObject> mapEntry : replies.entrySet()) {
                if (!replies.isEmpty() && mapEntry.getValue().get(ServerConstants.KIND).equals(ServerConstants.KIND_OK)) {
                    okReplies.add(mapEntry.getKey());
                    logger.debug("Responded OK to: " + getThisServerId() + " by: " + mapEntry.getKey());
                }
            }
            if (okReplies.isEmpty()) {
                // No one has responded; I am the leader
                announceToTheWorld();
                ServerState.getServerState().setCurrentLeader(new Leader(ServerState.getServerState().getServerFromId(getThisServerId())));
                logger.debug("No one responded to: " + getThisServerId());
                stopLeaderElection();
            } else {
                //Tell the
                sendElectedMessage(okReplies);
            }
        });
        leaderElectionThread.setDaemon(true);
        leaderElectionThread.setName("Leader-Election-Thread");
        leaderElectionThread.start();
    }

    public static void stopLeaderElection() {
        logger.debug("Stopping leader election");
        electionFlag = false;
        if (leaderElectionThread != null && leaderElectionThread.isAlive() && !leaderElectionThread.isInterrupted()) {
            leaderElectionThread.interrupt();
        }
//        else {
//            logger.debug("Leader election thread is either null, not alive or interrupted already");
//        }
        logger.debug("Elected Leader: " + ServerState.getServerState().getCurrentLeader());
    }

    private static String getThisServerId() {
        return ServerState.getServerState().getServerId();
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

    private static ConcurrentHashMap<String, JSONObject> sendElectionStartMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTION, getThisServerId());
        return Messaging.askServers(message, ServerState.getServerState().getServersHigherThanMyId()); //Only send election msg to higher servers with higher id
    }

    /**
     * Reply to an ELECTION message
     * Only replies OK if this server's server ID is larger than election starter's server ID
     *
     * @param request JSON request
     */
    public static void replyOKorPass(JSONObject request, Socket socket) {
        electionFlag = true;
        ServerState.getServerState().setCurrentLeader(null);
        String electionStarterId = (String) request.get(ServerConstants.SERVER_ID);

        if (Integer.parseInt(getThisServerId()) > Integer.parseInt(electionStarterId)) {
            JSONObject oKMessage = buildElectionJSON(ServerConstants.KIND_OK, getThisServerId());
            logger.debug("Sending OK message to: " + electionStarterId + " from: " + getThisServerId());
            Messaging.respond(oKMessage, socket);
        } else if (Integer.parseInt(getThisServerId()) < Integer.parseInt(electionStarterId)) {
            JSONObject passMessage = buildElectionJSON(ServerConstants.KIND_PASS, getThisServerId());
            logger.debug("Sending PASS message to: " + electionStarterId + " from: " + getThisServerId());
            Messaging.respond(passMessage, socket);
        } else {
            logger.error("Replying OK to self. This should not happen");
        }
    }


    /**
     * Send the "ELECTED" message to the winner of the election (Replied sever with the highest id)
     *
     * @param repliedIds JSON objects of replies
     */
    public static void sendElectedMessage(Collection<String> repliedIds) {
        int maxServerId = Integer.parseInt(getThisServerId());
        for (String j : repliedIds) {
            int serverId = Integer.parseInt(j);
            if (serverId > maxServerId) {
                maxServerId = serverId;
            }
        }
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Server maxServer = ServerState.getServerState().getServerFromId(Integer.toString(maxServerId));
        logger.debug("Max server: " + maxServer);
        Messaging.sendAndForget(new JSONObject(message), List.of(maxServer));
    }

    public static void respondToElectedMessage() {
        announceToTheWorld();
    }

    public static void announceToTheWorld() {
        logger.debug("Announcing to the world by: " + getThisServerId());
        JSONObject message = buildElectionJSON(ServerConstants.KIND_COORDINATOR, getThisServerId());
        Messaging.sendAndForget(new JSONObject(message), ServerState.getServerState().getServers());
    }

    public static void receiveCoordinator(JSONObject request) {
        // Update the current leader.
        // The election process is complete.
        String newLeaderId = (String) request.get(ServerConstants.SERVER_ID);
        if (electionFlag) {
            ServerState.getServerState().setCurrentLeader(new Leader(ServerState.getServerState().getServerFromId(newLeaderId)));
            stopLeaderElection();
        } else {
            logger.debug("Received coordinator sampler");
            if (Integer.parseInt(newLeaderId) < Integer.parseInt(getThisServerId())) {
                try {
                    // If more than one server, send the ELECTION message at the same time, election process may continue, each may elect themselves as leaders.
                    // To avoid this backoff for a random time and start the election again.
                    // Servers won't be stuck in a loop.
                    // Eventually they will agree on a new leader.
                    int sleepFor = new Random().nextInt(1500) + 100;
                    Thread.sleep(sleepFor);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
                logger.debug("Received COORDINATOR message after completing an election; " +
                        "Multiple servers may have started teh election process at the same time.");
                startElection();
            } else {
                ServerState.getServerState().setCurrentLeader(new Leader(ServerState.getServerState().getServerFromId(newLeaderId)));
                logger.debug("Elected Leader: " + ServerState.getServerState().getCurrentLeader());
            }
        }
    }
}
