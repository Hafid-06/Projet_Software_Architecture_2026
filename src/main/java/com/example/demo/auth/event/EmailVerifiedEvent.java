package com.example.demo.auth.event;

public class EmailVerifiedEvent {

    private String eventId;
    private String userId;
    private String occurredAt;

    public EmailVerifiedEvent() {}

    public EmailVerifiedEvent(String eventId, String userId, String occurredAt) {
        this.eventId    = eventId;
        this.userId     = userId;
        this.occurredAt = occurredAt;
    }

    public String getEventId()    { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId()     { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
}