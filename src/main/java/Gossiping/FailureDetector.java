package Gossiping;

import Constants.ServerProperties;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TimerTask;

public class FailureDetector extends TimerTask {
    private static final Logger logger = LogManager.getLogger(FailureDetector.class);

    @Override
    public void run() {
        // runs every 6s
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
            resetServerState();
        }
    }

    private static boolean detectPartition() {
        int totalServers = ServerState.getServerState().getServers().size() + 1;
        int smallPartitionMaxSize = totalServers / 2; // rounds down
        int failedServers = ServerState.getServerState().getFailedServers().size();
        return failedServers > smallPartitionMaxSize;
    }

    public static void recoverFromPartition(JSONObject request) {
        // this request contains the Leader's state
        // parse the request; update your state
        //todo
        ServerState.getServerState().setSmallPartitionFormed(false);
    }

    private static void resetServerState() {
        //todo
    }

}
