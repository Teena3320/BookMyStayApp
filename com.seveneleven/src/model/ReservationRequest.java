package model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class ReservationRequest {
    private final String requestId;
    private final String guestIdOrName;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final Instant submittedAt;

    private ReservationRequest(Builder b) {
        this.requestId = b.requestId == null ? generateId() : b.requestId;
        this.guestIdOrName = Objects.requireNonNull(b.guestIdOrName, "guestIdOrName");
        this.roomType = Objects.requireNonNull(b.roomType, "roomType").trim();
        this.checkIn = Objects.requireNonNull(b.checkIn, "checkIn");
        this.checkOut = Objects.requireNonNull(b.checkOut, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        this.submittedAt = b.submittedAt == null ? Instant.now() : b.submittedAt;
    }

    private static String generateId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getGuestIdOrName() {
        return guestIdOrName;
    }

    public String getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    @Override
    public String toString() {
        return "ReservationRequest{" +
                "requestId='" + requestId + '\'' +
                ", guest='" + guestIdOrName + '\'' +
                ", roomType='" + roomType + '\'' +
                ", checkIn=" + checkIn +
                ", checkOut=" + checkOut +
                ", submittedAt=" + submittedAt +
                '}';
    }

    public static class Builder {
        private String requestId;
        private String guestIdOrName;
        private String roomType;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private Instant submittedAt;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder guestIdOrName(String guestIdOrName) {
            this.guestIdOrName = guestIdOrName;
            return this;
        }

        public Builder roomType(String roomType) {
            this.roomType = roomType;
            return this;
        }

        public Builder checkIn(LocalDate checkIn) {
            this.checkIn = checkIn;
            return this;
        }

        public Builder checkOut(LocalDate checkOut) {
            this.checkOut = checkOut;
            return this;
        }

        public Builder submittedAt(Instant submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public ReservationRequest build() {
            return new ReservationRequest(this);
        }
    }
}