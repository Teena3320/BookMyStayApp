package services;

import model.InventorySnapshot;
import model.RoomView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SearchService {

    private final InventoryService inventory;

    public SearchService(InventoryService inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public List<RoomView> listAvailableRooms() {
        InventorySnapshot snapshot = inventory.snapshot();
        Map<String, Integer> counts = snapshot.getCounts();
        Map<String, Double> prices = snapshot.getPrices();

        List<RoomView> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            int count = e.getValue();
            if (count > 0) {
                String type = e.getKey();
                double price = prices.getOrDefault(type, 0.0d);
                result.add(new RoomView(type, count, price));
            }
        }
        result.sort(Comparator.comparing(RoomView::getRoomType, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public RoomView getRoomView(String roomType) {
        InventorySnapshot snapshot = inventory.snapshot();
        Integer count = snapshot.getCounts().get(roomType);
        Double price = snapshot.getPrices().get(roomType);
        if (count == null || price == null) return null;
        return new RoomView(roomType, count, price);
    }
}