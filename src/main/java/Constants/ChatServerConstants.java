package Constants;

public class ChatServerConstants {
    private ChatServerConstants(){

    }

    public class ServerConstants{
        public static final String TYPE_CLIENT = "Client";
        public static final String TYPE_SERVER = "Server";
    }

    public class ClientConstants {
        public static final String TYPE_CREATE_ID = "newidentity";
        public static final String TYPE_LIST = "list";
        public static final String TYPE_WHO = "who";
        public static final String TYPE_CREATE_ROOM = "createroom";
        public static final String TYPE_JOIN_ROOM = "joinroom";
        public static final String TYPE_MOVE_JOIN = "movejoin";
        public static final String TYPE_DELETE_ROOM = "deleteroom";
        public static final String TYPE_MESSAGE = "message";
        public static final String TYPE_QUIT = "quit";
    }

}
