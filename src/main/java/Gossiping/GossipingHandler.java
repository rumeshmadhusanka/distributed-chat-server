package Gossiping;

import org.json.simple.JSONObject;

public class GossipingHandler {

    /* TODO: Implement handler class to run on separate threads.
       Note: Implement communication related methods in Messaging class.
    * */

    public static void startGossiping(JSONObject jsonObject) {

        Thread gossipingThread = new Thread(() -> {

        });
        gossipingThread.setDaemon(true);
        gossipingThread.setName("Gossiping-Thread");
        gossipingThread.start();
    }
}
