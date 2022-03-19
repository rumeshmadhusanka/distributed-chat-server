package Gossiping;

import Constants.ServerProperties;
import Server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            if (System.currentTimeMillis() - entry.getValue() > ServerProperties.FAILURE_DETECTION_PERIOD && !entry.getKey().equals(ServerState.getServerState().getServerId())) {
                // mark the server as dead
                ServerState.getServerState().getHeartbeatMap().remove(entry.getKey());
                logger.error("Server failure detected through heartbeat. ServerId: " + entry.getKey());
                // TODO what to do after detecting a failure?
            }
        }
    }
}
