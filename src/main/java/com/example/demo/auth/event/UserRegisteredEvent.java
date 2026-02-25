package com.example.demo.auth.event;

public class UserRegisteredEvent {

    private String eventId;
    private String userId;
    private String email;
    private String tokenId;
    private String tokenClear;
    private String occurredAt;

    public UserRegisteredEvent() {}

    public UserRegisteredEvent(String eventId, String userId, String email,
                                String tokenId, String tokenClear, String occurredAt) {
        this.eventId    = eventId;
        this.userId     = userId;
        this.email      = email;
        this.tokenId    = tokenId;
        this.tokenClear = tokenClear;
        this.occurredAt = occurredAt;
    }

    public String getEventId()    { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId()     { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail()      { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTokenId()    { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }

    public String getTokenClear() { return tokenClear; }
    public void setTokenClear(String tokenClear) { this.tokenClear = tokenClear; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
}