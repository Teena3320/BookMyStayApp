package model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Reservation {
    private final String reservationId;
    private final String requestId;
    private final String guestIdOrName;
    private final String roomType;
    private final String roomId; 
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final double pricePerNight;
    private final double totalCost;
    private final ReservationStatus status;
    private final Instant createdAt;

    private Reservation(Builder b) {
        this.reservationId = b.reservationId == null ? genId() : b.reservationId;
        this.requestId = Objects.requireNonNull(b.requestId, "requestId");
        this.guestIdOrName = Objects.requireNonNull(b.guestIdOrName, "guestIdOrName");
        this.roomType = Objects.requireNonNull(b.roomType, "roomType");
        this.roomId = Objects.requireNonNull(b.roomId, "roomId");
        this.checkIn = Objects.requireNonNull(b.checkIn, "checkIn");
        this.checkOut = Objects.requireNonNull(b.checkOut, "checkOut");
        this.nights = b.nights;
        this.pricePerNight = b.pricePerNight;
        this.totalCost = b.totalCost;
        this.status = b.status == null ? ReservationStatus.CONFIRMED : b.status;
        this.createdAt = b.createdAt == null ? Instant.now() : b.createdAt;
    }

    private static String genId() {
        return "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getReservationId() {
        return reservationId;
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

    public String getRoomId() {
        return roomId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "reservationId='" + reservationId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", guest='" + guestIdOrName + '\'' +
                ", roomType='" + roomType + '\'' +
                ", roomId='" + roomId + '\'' +
                ", checkIn=" + checkIn +
                ", checkOut=" + checkOut +
                ", nights=" + nights +
                ", pricePerNight=" + pricePerNight +
                ", totalCost=" + totalCost +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }

    public static class Builder {
        private String reservationId;
        private String requestId;
        private String guestIdOrName;
        private String roomType;
        private String roomId;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private int nights;
        private double pricePerNight;
        private double totalCost;
        private ReservationStatus status;
        private Instant createdAt;

        public Builder reservationId(String reservationId) { this.reservationId = reservationId; return this; }
        public Builder requestId(String requestId) { this.requestId = requestId; return this; }
        public Builder guestIdOrName(String guestIdOrName) { this.guestIdOrName = guestIdOrName; return this; }
        public Builder roomType(String roomType) { this.roomType = roomType; return this; }
        public Builder roomId(String roomId) { this.roomId = roomId; return this; }
        public Builder checkIn(LocalDate checkIn) { this.checkIn = checkIn; return this; }
        public Builder checkOut(LocalDate checkOut) { this.checkOut = checkOut; return this; }
        public Builder nights(int nights) { this.nights = nights; return this; }
        public Builder pricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; return this; }
        public Builder totalCost(double totalCost) { this.totalCost = totalCost; return this; }
        public Builder status(ReservationStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public Reservation build() {
            return new Reservation(this);
        }
    }
}