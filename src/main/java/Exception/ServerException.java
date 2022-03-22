package Exception;

public class ServerException extends Exception {
    private String code;

    public ServerException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
