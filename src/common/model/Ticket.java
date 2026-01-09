package common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { OPEN, IN_PROGRESS, CLOSED }

    private int id;
    private String title;
    private String description;
    private Status status;
    private String owner; // Username of creator
    private LocalDateTime createdAt;
    
    // Default constructor
    public Ticket() {}

    public Ticket(int id, String title, String description, String owner) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.owner = owner;
        this.status = Status.OPEN;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - %s", id, title, status, owner);
    }
}
