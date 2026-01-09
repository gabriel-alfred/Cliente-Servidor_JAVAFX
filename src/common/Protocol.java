package common;

public class Protocol {
    // Configuration
    public static final int PORT = 9000;

    // Command Types (Requests)
    public static final int CMD_LOGIN = 1;
    public static final int CMD_LIST_TICKETS = 2;
    public static final int CMD_CREATE_TICKET = 3;
    public static final int CMD_UPDATE_TICKET = 4;
    public static final int CMD_LOGOUT = 5;

    // Response Status
    public static final int STATUS_OK = 200;
    public static final int STATUS_ERROR = 400;
    public static final int STATUS_UNAUTHORIZED = 401;

    // Error Messages
    public static final String ERR_LOGIN_FAILED = "Credenciales incorrectas";
    public static final String ERR_USER_EXISTS = "El usuario ya existe";
}
