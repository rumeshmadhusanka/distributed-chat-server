package Consensus;

import Constants.ChatServerConstants.ServerConstants;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class LeaderElection {
    private static final Logger logger = LogManager.getLogger(LeaderElection.class);
    public static boolean electionStarter = false;
    public static boolean electionFlag = false; // an election is running
    private static final Collection<String> okMessageIDList = new ArrayList<>();
    private static final Object lock = new Object();
    private static TimerTask timerTask = null;
    private static Thread leaderElectionThread = null;

    private enum electionState {NO_ELECTION, ELECTION_STARTED,}

    //No objects from LeaderElection class
    private LeaderElection() {

    }

    public static void startElection() {
        if (electionFlag) {
            logger.debug("An election process is already running");
            return;
        }
        electionFlag = true;
        leaderElectionThread = new Thread(() -> {
            ConcurrentHashMap<String, JSONObject> replies = sendElectionStartMessage();
            if (replies.isEmpty()) {
                // No one has responded; I am the leader
                announceToTheWorld();
                ServerState.getServerState().setCurrentLeader(new Leader(ServerState.getServerState().getServerFromId(getThisServerId())));
                logger.trace("No one responded to: "+getThisServerId());
                stopLeaderElection();
            } else {
                Collection<String> r = new ArrayList<>();

                for (Iterator<String> it = replies.keys().asIterator(); it.hasNext(); ) {
                    String s = it.next();
                    r.add(s);
                }
                logger.trace("Responded OK to: "+getThisServerId()+" by: "+r);
                sendElectedMessage(r);
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


//    private static void continueElection(){
//        if (okMessageIDList.isEmpty()) {
//            // I am the leader; Tell the world
//            announceToTheWorld();
//        } else {
//            // Someone else has replied
//            sendElectedMessage(okMessageIDList);
//        }
//    }

    private static String getThisServerId() {
        return ServerState.getServerState().getServerId();
    }

//    private static Collection<Server> getAllServersExceptMe() {
//        List<Server> allServersExcept = new ArrayList<>();
//        for (Server s : ) {
//            if (!getThisServerId().equals(s.getId())) {
//                allServersExcept.add(s);
//            }
//        }
//        return allServersExcept;
//    }

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
        return Messaging.askServers(message, ServerState.getServerState().getServers());
    }

//    private static void startOKMessageTimer() {
//        timerTask = new java.util.TimerTask() {
//            @Override
//            public void run() {
//                continueElection();
//            }
//        };
//        new Timer(true).schedule(timerTask, ServerProperties.CONN_TIMEOUT);
//    }

    /**
     * Reply to an ELECTION message
     * Only replies OK if this server's server ID is larger than election starter's server ID
     *
     * @param request JSON request
     */
    public static void replyOK(JSONObject request, Socket socket) throws IOException {
        electionFlag = true;

        String electionStarterId = (String) request.get(ServerConstants.SERVER_ID);
        JSONObject message = buildElectionJSON(ServerConstants.KIND_OK, getThisServerId());
//        for (Server s : ServerState.getServerState().getServers()) {
        if (Integer.parseInt(getThisServerId()) > Integer.parseInt(electionStarterId)) {
            logger.trace("Sending OK message to: " + electionStarterId + " from: " + getThisServerId());
            Messaging.respond(message, socket);
        }
//        }
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
//        if (maxServerId == Integer.parseInt(getThisServerId())) {
//            logger.error("Replied server has a lower id than the election starter. No one else has replied either. This should not happen");
//            logger.info("Electing self as the leader");
//            announceToTheWorld(); //TODO check this
//        } else {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Server maxServer = ServerState.getServerState().getServerFromId(Integer.toString(maxServerId));
        logger.trace("Max server: " + maxServer);
        Messaging.sendAndForget(new JSONObject(message), List.of(maxServer));
//        }
    }

    public static void receiveElectedMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Predicate<Server> higherServerPredicate = server -> Integer.parseInt(server.getId()) > Integer.parseInt(getThisServerId());
        Collection<Server> higherServers = ServerState.getServerState().getServers().stream().filter(higherServerPredicate).collect(Collectors.toList());
        if (!higherServers.isEmpty()) {
            Messaging.sendAndForget(new JSONObject(message), higherServers);
        } else {
            logger.debug("I am the server with the highest server ID: " + getThisServerId());
        }
    }

    public static void respondToElectedMessage() {
        announceToTheWorld();
    }

    public static void announceToTheWorld() {
        logger.trace("Announcing to the world by: " + getThisServerId());
        JSONObject message = buildElectionJSON(ServerConstants.KIND_COORDINATOR, getThisServerId());
        Messaging.sendAndForget(new JSONObject(message), ServerState.getServerState().getServers());
    }

    public static void receiveCoordinator(JSONObject request) {
        // Update the current leader.
        // The election process is complete.
        String newLeaderId = (String) request.get(ServerConstants.SERVER_ID);
//        List<Server> leader = getAllServersExceptMe().stream().filter((Server s) -> s.getId().equals(newLeaderId)).collect(Collectors.toList());
//        assert !leader.isEmpty(); //TODO add new exception code?
//        ServerState.getServerState().setCurrentLeader(new Leader(leader.get(0)));//cast to Leader type
//        if (Integer.parseInt(newLeaderId) < Integer.parseInt(getThisServerId())) {
//            startElection();
//            return;
//        }
        ServerState.getServerState().setCurrentLeader(new Leader(ServerState.getServerState().getServerFromId(newLeaderId)));
        stopLeaderElection();

    }

//    private static void cancelOKTimer(){
//        if (timerTask != null){
//            timerTask.cancel();
//        }
//    }


//    /**
//     * Add a server ID to the ok message list
//     *
//     * @param request JSON request
//     */
//    public static void addToOKMsgList(JSONObject request) {
//        synchronized (lock) {
//            String serverId = (String) request.get(ServerConstants.SERVER_ID);
//            okMessageIDList.add(serverId);
//
//            int higherServers = (int) getAllServersExceptMe().stream().filter((Server s)->Integer.parseInt(s.getId())>Integer.parseInt(getThisServerId())).count();
//            if (okMessageIDList.size()==higherServers){
//                cancelOKTimer();
//                continueElection();
//            }
//        }
//    }

//    /**
//     * Clear the OK message list after timeout
//     */
//    private static void clearOKMsgList() {
//        synchronized (lock) {
//            okMessageIDList.clear();
//        }
//    }


}
