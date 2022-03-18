package Constants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class ServerProperties {
    public static long CONN_TIMEOUT;
    public static int THREAD_COUNT;
    public static final String LOCAL_ADDRESS = "0.0.0.0";
    public static int HEARTBEAT_PERIOD;
    public static int FAILURE_DETECTION_PERIOD;


    private static final Logger logger = LogManager.getLogger(ServerProperties.class);

    public static void init() {
        try {
            Properties prop = new Properties();
            prop.load(ServerProperties.class.getClassLoader().getResourceAsStream("config.properties"));

            CONN_TIMEOUT = Long.parseLong(prop.getProperty("connection.timeout"));
            THREAD_COUNT = Integer.parseInt(prop.getProperty("messaging.executor.threads"));
            HEARTBEAT_PERIOD = Integer.parseInt(prop.getProperty("gossiping.heartbeat.period"));
            FAILURE_DETECTION_PERIOD = Integer.parseInt(prop.getProperty("gossiping.failure-detection.period"));

        } catch (IOException | NumberFormatException e) {
            logger.fatal("Properties file error");
            System.exit(1);
        }
    }

}