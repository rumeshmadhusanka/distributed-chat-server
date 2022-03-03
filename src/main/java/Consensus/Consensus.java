package Consensus;

import Server.ServerState;

public class Consensus {

    public static boolean verifyIdentity(String identity) {
        Leader leader = ServerState.getServerState().getCurrentLeader();
        if(ServerState.getServerState().getServerId().equals(leader.getId())){

        } else {
            // Contact leader.
        }
        return true;
    }

}
