package model;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class RoomView {
    private final String roomType;
    private final int availableCount;
    private final double pricePerNight;
    private final Set<String> amenities; 

    public RoomView(String roomType, int availableCount, double pricePerNight) {
        this(roomType, availableCount, pricePerNight, Collections.emptySet());
    }

    public RoomView(String roomType, int availableCount, double pricePerNight, Set<String> amenities) {
        this.roomType = Objects.requireNonNull(roomType, "roomType");
        this.availableCount = availableCount;
        this.pricePerNight = pricePerNight;
        this.amenities = amenities == null ? Collections.emptySet() : Collections.unmodifiableSet(amenities);
    }

    public String getRoomType() {
        return roomType;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public Set<String> getAmenities() {
        return amenities;
    }

    @Override
    public String toString() {
        return "RoomView{" +
                "roomType='" + roomType + '\'' +
                ", availableCount=" + availableCount +
                ", pricePerNight=" + pricePerNight +
                ", amenities=" + amenities +
                '}';
    }
}