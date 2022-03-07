package Consensus;

import Constants.ChatServerConstants.ServerConstants;
import Constants.ServerProperties;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class LeaderElection {
    private static final Logger logger = LogManager.getLogger(LeaderElection.class);
    public static boolean electionStarter = false;
    public static boolean electionFlag = false;
    private static final Collection<String> okMessageIDList = new ArrayList<>();
    private static final Object lock = new Object();
    private static TimerTask timerTask = null;

    //No objects from LeaderElection class
    private LeaderElection() {

    }

    public static void startElection() {
        electionStarter = true;
        sendElectionStartMessage();
        startOKMessageTimer();
    }


    private static void continueElection(){
        if (okMessageIDList.isEmpty()) {
            // I am the leader; Tell the world
            announceToTheWorld();
        } else {
            // Someone else has replied
            sendElectedMessage(okMessageIDList);
            endElection();
        }
    }

    private static String getThisServerId() {
        return ServerState.getServerState().getServerId();
    }

    private static Collection<Server> getAllServers() {
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

    private static void sendElectionStartMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTION, getThisServerId());
        Messaging.sendAndForget(message, ServerState.getServerState().getServers());
    }

    private static void startOKMessageTimer() {
        timerTask = new java.util.TimerTask() {
            @Override
            public void run() {
                continueElection();
            }
        };
        new Timer(true).schedule(timerTask, ServerProperties.CONN_TIMEOUT);
    }

    /**
     * Reply to an ELECTION message
     * Only replies OK if this server's server ID is larger than election starter's server ID
     *
     * @param request JSON request
     */
    public static void replyOK(JSONObject request) {
        String electionStarterId = (String) request.get(ServerConstants.SERVER_ID);
        JSONObject message = buildElectionJSON(ServerConstants.KIND_OK, getThisServerId());
        for (Server s : ServerState.getServerState().getServers()) {
            if (s.getId().equals(electionStarterId) &&
                    Integer.parseInt(getThisServerId()) > Integer.parseInt(electionStarterId)) {
                logger.debug("Sending OK message to: " + electionStarterId + " from: " + getThisServerId());
                Messaging.sendAndForget(new JSONObject(message), List.of(s));
                return;
            }
        }
        logger.error("This line should never reached; OK message sent by a non existing server");
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
        if (maxServerId == Integer.parseInt(getThisServerId())) {
            logger.error("Replied server has a lower id than the election starter. No one else has replied either. This should not happen");
            logger.info("Electing self as the leader");
            announceToTheWorld(); //TODO check this
        } else {
            JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
            int finalMaxServerId = maxServerId;
            Server maxServer = getAllServers().stream().filter((Server s) -> s.getId().equals(Integer.toString(finalMaxServerId))).findFirst().orElse(null);
            assert maxServer != null;
            Messaging.sendAndForget(new JSONObject(message), List.of(maxServer));
        }
    }

    public static void receiveElectedMessage() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_ELECTED, getThisServerId()); //sent by this server
        Predicate<Server> higherServerPredicate = server -> Integer.parseInt(server.getId()) > Integer.parseInt(getThisServerId());
        Collection<Server> higherServers = getAllServers().stream().filter(higherServerPredicate).collect(Collectors.toList());
        if (!higherServers.isEmpty()) {
            Messaging.sendAndForget(new JSONObject(message), higherServers);
        } else {
            logger.debug("I am the server with the highest server ID: " + getThisServerId());
        }
    }

    public static void announceToTheWorld() {
        JSONObject message = buildElectionJSON(ServerConstants.KIND_COORDINATOR, getThisServerId());
        Messaging.sendAndForget(new JSONObject(message), getAllServers());
    }

    public static void receiveCoordinator(JSONObject request) {
        // Update the current leader.
        // The election process is complete.
        String newLeaderId = (String) request.get(ServerConstants.SERVER_ID);
        List<Server> leader = getAllServers().stream().filter((Server s) -> s.getId().equals(newLeaderId)).collect(Collectors.toList());
        assert !leader.isEmpty(); //TODO add new exception code?
        ServerState.getServerState().setCurrentLeader((Leader) leader.get(0));//cast to Leader type
        endElection();
    }

    private static void cancelOKTimer(){
        if (timerTask != null){
            timerTask.cancel();
        }
    }

    private static void endElection() {
        clearOKMsgList();
        electionFlag = false;
        electionStarter = false;
        cancelOKTimer();
    }

    /**
     * Add a server ID to the ok message list
     *
     * @param request JSON request
     */
    public static void addToOKMsgList(JSONObject request) {
        synchronized (lock) {
            String serverId = (String) request.get(ServerConstants.SERVER_ID);
            okMessageIDList.add(serverId);

            int higherServers = (int) getAllServers().stream().filter((Server s)->Integer.parseInt(s.getId())>Integer.parseInt(getThisServerId())).count();
            if (okMessageIDList.size()==higherServers){
                cancelOKTimer();
                continueElection();
            }
        }
    }

    /**
     * Clear the OK message list after timeout
     */
    private static void clearOKMsgList() {
        synchronized (lock) {
            okMessageIDList.clear();
        }
    }


}
