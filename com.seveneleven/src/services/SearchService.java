package services;

import model.RoomView;

import java.time.LocalDate;
import java.util.*;

public class SearchService {

    private final InventoryService inventory;

    public SearchService(InventoryService inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public List<RoomView> listAvailableRooms(LocalDate startInclusive, LocalDate endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must be after startInclusive");
        }

        Map<String, Integer> baseCounts = inventory.snapshotCounts();
        Map<String, Double> prices = inventory.snapshotPrices();

        List<RoomView> result = new ArrayList<>();
        for (String type : baseCounts.keySet()) {
            int minAvail = inventory.getAvailableForRange(type, startInclusive, endExclusive);
            if (minAvail > 0) {
                double price = prices.getOrDefault(type, 0.0);
                result.add(new RoomView(type, minAvail, price));
            }
        }
        result.sort(Comparator.comparing(RoomView::getRoomType, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public int getAvailabilityForRange(String type, LocalDate startInclusive, LocalDate endExclusive) {
        return inventory.getAvailableForRange(type, startInclusive, endExclusive);
    }
    
    public int getAvailabilityOnDate(String type, LocalDate date) {
        return inventory.getAvailableOnDate(type, date);
    }
}