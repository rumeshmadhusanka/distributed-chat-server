package Gossiping;

import Constants.ChatServerConstants;
import Constants.ServerProperties;
import Server.Room;
import Server.ServerState;
import Utilities.Messaging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FailureDetector extends TimerTask {
    private static final Logger logger = LogManager.getLogger(FailureDetector.class);

    @Override
    public void run() {
        // runs every 6s
        try {
            Set<Entry<String, Long>> entries = ServerState.getServerState().getHeartbeatMap().entrySet();
            for (Entry<String, Long> entry : entries) {
                if (System.currentTimeMillis() - entry.getValue() > ServerProperties.FAILURE_DETECTION_PERIOD &&
                        !entry.getKey().equals(ServerState.getServerState().getServerId())) {
                    // mark the server as dead
                    logger.error("Server failure detected through heartbeat. ServerId: " + entry.getKey());
                    Gossiping.removeServer(entry.getKey());
                }
            }
            if (detectPartition()) {
                //I'm in the small partition;reset my server state
                logger.info("This server is in a small partition.");
                ServerState.getServerState().setSmallPartitionFormed(true);
                ServerState.getServerState().purgeServerState();
            } else {
                // Start election()
                ConcurrentLinkedQueue<String> failedServersId = ServerState.getServerState().getFailedServers();
                if (failedServersId.size() != 0) {
                    logger.trace("Partition has created. This server is in the large partition");
                    if (ServerState.getServerState().amITheLeader()) {
                        logger.info("Removing identities and rooms of servers in small partition.");
                        for (String serverId : failedServersId) {
                            // Remove rooms of small partition.
                            Collection<Room> roomsList = ServerState.getServerState().getRoomsByServer(serverId);
                            for (Room room : roomsList) {
                                ServerState.getServerState().removeRoom(room);
                                Messaging.informServersRoom(ChatServerConstants.ServerConstants.KIND_INFORM_DELETE_ROOM, room.getRoomId(), room.getOwner());
                            }
                            // Remove identities of small partition.
                            Collection<String> idList = ServerState.getServerState().getIdentityByServer(serverId);
                            for (String id : idList) {
                                ServerState.getServerState().removeIdentity(id);
                                Messaging.informServersIdentity(ChatServerConstants.ServerConstants.KIND_INFORM_DELETE_IDENTITY, id);
                            }
                        }
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.debug(e);
        }
    }

    private static boolean detectPartition() {
        int totalServers = ServerState.getServerState().getServers().size() + 1;
        int smallPartitionMaxSize = totalServers / 2; // rounds down
        int failedServers = ServerState.getServerState().getFailedServers().size();
        return failedServers > smallPartitionMaxSize;
    }
}
