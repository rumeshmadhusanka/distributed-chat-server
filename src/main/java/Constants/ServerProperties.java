package Constants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class ServerProperties {
    public static long CONN_TIMEOUT;


    private static final Logger logger = LogManager.getLogger(ServerProperties.class);
    public static void init() {
        try {
            Properties prop = new Properties();
            prop.load(ServerProperties.class.getClassLoader().getResourceAsStream("config.properties"));

            CONN_TIMEOUT = Long.parseLong(prop.getProperty("connection.timeout"));
        } catch (IOException | NumberFormatException e) {
            logger.fatal("Properties file error");
            System.exit(1);
        }
    }

}