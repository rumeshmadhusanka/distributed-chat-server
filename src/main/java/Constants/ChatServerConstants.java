package Constants;

public class ChatServerConstants {
    private ChatServerConstants(){

    }

    /**
     * Constants related to Server.
     */
    public static class ServerConstants{

        public static final String MAIN_HALL = "MainHall";
        public static final String UNIQUE = "unique";

        public static final String TYPE = "type";
        public static final String TYPE_CONSENSUS = "consensus";
        public static final String TYPE_GOSSIP = "gossip";

        public static final String KIND = "kind";
        public static final String KIND_VERIFY_UNIQUE = "verifyunique";

        public static final String KIND_REQUEST_TO_CREATE_NEW_IDENTITY = "requesttocreatenewidentity";
        public static final String KIND_REPLY_TO_CREATE_NEW_IDENTITY = "replytocreatenewidentity";

        public static final String KIND_REQUEST_TO_CREATE_NEW_ROOM = "requesttocreatenewroom";
        public static final String KIND_REPLY_TO_CREATE_NEW_ROOM = "replytocreatenewroom";

        public static final String KIND_INFORM_NEW_IDENTITY = "informnewidentity";
        public static final String KIND_INFORM_DELETE_IDENTITY = "informdeleteidentity";

        public static final String KIND_INFORM_NEW_ROOM = "informnewroom";
        public static final String KIND_INFORM_DELETE_ROOM = "informdeleteroom";

        public static final String IDENTITY = "identity";
        public static final String ROOM_ID = "roomid";
    }

    public static class BullyConstants {

        public static final String SERVER_ID = "serverid";

        public static final String TYPE = "type";
        public static final String TYPE_BULLY = "bully";

        public static final String KIND = "kind";
        public static final String KIND_ELECTION = "election";
        public static final String KIND_OK = "ok";
        public static final String KIND_ELECTED = "elected";
        public static final String KIND_COORDINATOR = "coordinator";
    }

    /**
     * Constants related to Client
     */
    public static class ClientConstants {
        public static final String TYPE = "type";
        public static final String TYPE_CREATE_ID = "newidentity";
        public static final String TYPE_LIST = "list";
        public static final String TYPE_WHO = "who";
        public static final String TYPE_CREATE_ROOM = "createroom";
        public static final String TYPE_JOIN_ROOM = "joinroom";
        public static final String TYPE_MOVE_JOIN = "movejoin";
        public static final String TYPE_DELETE_ROOM = "deleteroom";
        public static final String TYPE_MESSAGE = "message";
        public static final String TYPE_QUIT = "quit";

        public static final String APPROVED = "approved";
        public static final String TRUE = "true";
        public static final String FALSE = "false";

        public static final String IDENTITY = "identity";
        public static final String ROOM_ID = "roomid";

    }

    public static class ServerExceptionConstants {
        public static final String LEADER_FAILED_MSG = "Leader failed.";
        public static final String LEADER_FAILED_CODE = "SE-01";
    }

}
