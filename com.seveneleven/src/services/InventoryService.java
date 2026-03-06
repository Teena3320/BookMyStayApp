package services;

import model.InventorySnapshot;
import util.ReadWriteLockGuard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class InventoryService {

    private final Map<String, Integer> availableCounts = new HashMap<>();
    private final Map<String, Double> pricesPerNight = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void addRoomType(String type, int initialCount, double pricePerNight) {
        String key = normalizeType(type);
        validateNonNegativeCount(initialCount);
        validateNonNegativePrice(pricePerNight);

        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            if (availableCounts.containsKey(key) || pricesPerNight.containsKey(key)) {
                throw new IllegalArgumentException("Room type already exists: " + key);
            }
            availableCounts.put(key, initialCount);
            pricesPerNight.put(key, pricePerNight);
        }
    }

    public void updateCount(String type, int delta) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            ensureTypeExists(key);
            int current = availableCounts.get(key);
            long newValue = (long) current + delta; // guard overflow
            if (newValue < 0) {
                throw new IllegalArgumentException("Resulting count cannot be negative for type: " + key);
            }
            availableCounts.put(key, (int) newValue);
        }
    }

    public void updatePrice(String type, double pricePerNight) {
        String key = normalizeType(type);
        validateNonNegativePrice(pricePerNight);
        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            ensureTypeExists(key);
            pricesPerNight.put(key, pricePerNight);
        }
    }

    public int getAvailableCount(String type) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            ensureTypeExists(key);
            return availableCounts.get(key);
        }
    }

    public double getPrice(String type) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            ensureTypeExists(key);
            return pricesPerNight.get(key);
        }
    }

    public Map<String, Integer> snapshotCounts() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return Collections.unmodifiableMap(new HashMap<>(availableCounts));
        }
    }

    public Map<String, Double> snapshotPrices() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return Collections.unmodifiableMap(new HashMap<>(pricesPerNight));
        }
    }

    public InventorySnapshot snapshot() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return new InventorySnapshot(availableCounts, pricesPerNight);
        }
    }

    public boolean hasRoomType(String type) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return availableCounts.containsKey(key) && pricesPerNight.containsKey(key);
        }
    }

    private static String normalizeType(String type) {
        Objects.requireNonNull(type, "room type cannot be null");
        String t = type.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("room type cannot be blank");
        }
        return t;
    }

    private static void validateNonNegativeCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
    }

    private static void validateNonNegativePrice(double price) {
        if (price < 0.0d) {
            throw new IllegalArgumentException("price cannot be negative");
        }
    }

    private void ensureTypeExists(String key) {
        if (!availableCounts.containsKey(key) || !pricesPerNight.containsKey(key)) {
            throw new IllegalArgumentException("Unknown room type: " + key);
        }
    }
}