package model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class InventorySnapshot {

    private final Map<String, Integer> counts;
    private final Map<String, Double> prices;

    public InventorySnapshot(Map<String, Integer> counts, Map<String, Double> prices) {
        Objects.requireNonNull(counts, "counts cannot be null");
        Objects.requireNonNull(prices, "prices cannot be null");
        // Defensive copies and unmodifiable wrappers
        this.counts = Collections.unmodifiableMap(new HashMap<>(counts));
        this.prices = Collections.unmodifiableMap(new HashMap<>(prices));
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public Map<String, Double> getPrices() {
        return prices;
    }

    @Override
    public String toString() {
        return "InventorySnapshot{" +
                "counts=" + counts +
                ", prices=" + prices +
                '}';
    }
}