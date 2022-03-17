package Consensus;

import Constants.ChatServerConstants.ServerConstants;
import Constants.ChatServerConstants.ServerExceptionConstants;
import Exception.ServerException;
import Messaging.Messaging;
import Server.Server;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Consensus {

    private static final Logger logger = LogManager.getLogger(Consensus.class);

    /**
     * Whether this server is the current leader
     *
     * @return boolean
     */
    private static boolean isLeader() throws ServerException {
        Leader currentLeader = ServerState.getServerState().getCurrentLeader();
        if (currentLeader == null) {
            logger.info("No elected leader present in ServerState.");
            throw new ServerException(
                    ServerExceptionConstants.NO_LEADER_MSG,
                    ServerExceptionConstants.NO_LEADER_CODE);
        }
        logger.debug("Leader id: " + currentLeader.getId());
        logger.debug("Self id: " + ServerState.getServerState().getServerId());
        return ServerState.getServerState().getServerId().equals(currentLeader.getId());
    }

    /**
     * Verify whether a given value is unique across all servers.
     *
     * @param value   the value you want to verify whether it is unique or not
     * @param askType room | identity
     */
    public static boolean verifyUniqueValue(String value, String askType) throws IOException, ParseException, ServerException, InterruptedException {
        boolean isUnique = true;
        HashMap<String, String> request;
        try {
            if (isLeader()) {
                logger.debug("Taking 'Is the leader path'.");
                // build the request
                request = createRequestMap();
                request.put(ServerConstants.KIND, ServerConstants.KIND_VERIFY_UNIQUE);
                switch (askType) {
                    case ServerConstants.IDENTITY:
                        request.put(ServerConstants.IDENTITY, value);
                        break;
                    case ServerConstants.ROOM_ID:
                        request.put(ServerConstants.ROOM_ID, value);
                }
                Collection<Server> servers = ServerState.getServerState().getServers();
                ConcurrentHashMap<String, JSONObject> responses = Messaging.askServers(new JSONObject(request), servers);
                logger.debug("Response object for Ask Servers:" + (responses.values()));
                for (JSONObject response : responses.values()) {
                    if (!Boolean.parseBoolean((String) response.get("unique"))) {
                        isUnique = false;
                        break;
                    }
                }
            } else {

                logger.debug("Taking 'Not the leader path'.");
                // Get current leader.
                Leader currentLeader = ServerState.getServerState().getCurrentLeader();

                // Throw ServerException if no leader exists.
                if (currentLeader == null) {
                    logger.info("No elected leader present in ServerState.");
                    throw new ServerException(
                            ServerExceptionConstants.NO_LEADER_MSG,
                            ServerExceptionConstants.NO_LEADER_CODE);
                }

                // Create Request JSON.
                request = createRequestMap();
                switch (askType) {
                    case ServerConstants.IDENTITY:
                        request.put(ServerConstants.KIND, ServerConstants.KIND_REQUEST_TO_CREATE_NEW_IDENTITY);
                        request.put(ServerConstants.IDENTITY, value);
                        break;
                    case ServerConstants.ROOM_ID:
                        request.put(ServerConstants.KIND, ServerConstants.KIND_REQUEST_TO_CREATE_NEW_ROOM);
                        request.put(ServerConstants.ROOM_ID, value);
                }
                request.put(ServerConstants.SERVER_ID, ServerState.getServerState().getServerId());

                // Contact leader for verification.
                logger.debug("Contacting Leader for verification");
                JSONObject response = Messaging.contactLeader(new JSONObject(request), currentLeader);

                // Return response received from the leader.
                String responseKind = (String) response.get(ServerConstants.KIND);
                if (responseKind.equals(ServerConstants.KIND_REPLY_TO_CREATE_NEW_IDENTITY) ||
                        responseKind.equals(ServerConstants.KIND_REPLY_TO_CREATE_NEW_ROOM)) {
                    isUnique = Boolean.parseBoolean((String) response.get(ServerConstants.SUCCESS));
                }

            }
        } catch (ServerException err) {
            // Start leader election if
            if (err.getCode().equals(ServerExceptionConstants.LEADER_FAILED_CODE) ||
                    err.getCode().equals(ServerExceptionConstants.NO_LEADER_CODE)) {
                logger.debug("Leader either doesn't exist or failed. Starting Leader Election Process.");
                LeaderElection.startElection();
                // TODO: Need to set an exit block the recursion. Take num of attempts into consideration.
                logger.debug("Restarting verification process. Attempt: ");
                Thread.sleep(5000);
                return verifyUniqueValue(value, askType);
            } else {
                throw err;
            }
        }
        return isUnique;
    }

    /**
     * Initialize a hashmap with TYPE as "consensus"
     *
     * @return - HashMap
     */
    private static HashMap<String, String> createRequestMap() {
        HashMap<String, String> request = new HashMap<>();
        request.put(ServerConstants.TYPE, ServerConstants.TYPE_CONSENSUS);
        return request;
    }
}
