package Gossiping;

import Constants.ServerProperties;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

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
                ServerState.getServerState().setSmallPartitionFormed(true);
                ServerState.getServerState().purgeServerState();
            }
            // TODO: Larger partition needs to remove identities and rooms of failed servers.
            // Start election()

        } catch (IOException e) {
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
