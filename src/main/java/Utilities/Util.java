package Utilities;

import Server.Server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class Util {

    public static Collection<Server> getRandomServers(Collection<Server> servers, int n) {
        ArrayList<Server> serversList = new ArrayList<>(servers);
        Random random = new Random();
        Collection<Server> randomServers = new ArrayList<>();
        //select servers randomly
        for (int i = 0; i < n; i++) {
            randomServers.add(serversList.get(random.nextInt(serversList.size())));
        }
        return randomServers;
    }
}
