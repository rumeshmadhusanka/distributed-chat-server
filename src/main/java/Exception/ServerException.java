package Exception;

public class ServerException extends Exception {
    private String code;

    public ServerException(String message, String code) {
        super(message);
        this.code = code;
    }

    public ServerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public String getCode() {
        return code;
    }
}
