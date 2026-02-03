package common.model;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private int command;      // Protocol.CMD_... or Protocol.STATUS_...
    private Object object;    // Payload (User, Ticket, List<Ticket>, String error, etc.)

    public Message(int command, Object object) {
        this.command = command;
        this.object = object;
    }

    public int getCommand() { return command; }
    public void setCommand(int command) { this.command = command; }

    public Object getObject() { return object; }
    public void setObject(Object object) { this.object = object; }
}
