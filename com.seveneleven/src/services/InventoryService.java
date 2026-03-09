package services;

import model.InventorySnapshot;
import util.ReadWriteLockGuard;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InventoryService {

    private final Map<String, Integer> baseCounts = new HashMap<>();
    private final Map<String, Double> pricesPerNight = new HashMap<>();

    private final Map<String, Map<LocalDate, Integer>> bookedByTypeAndDate = new HashMap<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void addRoomType(String type, int initialCount, double pricePerNight) {
        String key = normalizeType(type);
        validateNonNegativeCount(initialCount);
        validateNonNegativePrice(pricePerNight);

        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            if (baseCounts.containsKey(key) || pricesPerNight.containsKey(key)) {
                throw new IllegalArgumentException("Room type already exists: " + key);
            }
            baseCounts.put(key, initialCount);
            pricesPerNight.put(key, pricePerNight);
            bookedByTypeAndDate.putIfAbsent(key, new HashMap<>());
        }
    }

    public void updateCount(String type, int delta) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            ensureTypeExists(key);
            int current = baseCounts.get(key);
            long newBaseL = (long) current + delta;
            if (newBaseL < 0) {
                throw new IllegalArgumentException("Resulting base count cannot be negative for type: " + key);
            }
            int newBase = (int) newBaseL;

            int maxBooked = getMaxBookedLocked(key);
            if (newBase < maxBooked) {
                throw new IllegalArgumentException(
                        "Cannot reduce base below already-booked maximum (" + maxBooked + ") for type: " + key);
            }
            baseCounts.put(key, newBase);
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
            return baseCounts.get(key);
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
            return Collections.unmodifiableMap(new HashMap<>(baseCounts));
        }
    }

    public Map<String, Double> snapshotPrices() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return Collections.unmodifiableMap(new HashMap<>(pricesPerNight));
        }
    }

    public InventorySnapshot snapshot() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return new InventorySnapshot(baseCounts, pricesPerNight);
        }
    }

    public boolean hasRoomType(String type) {
        String key = normalizeType(type);
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            return baseCounts.containsKey(key) && pricesPerNight.containsKey(key);
        }
    }

    public int getAvailableOnDate(String type, LocalDate date) {
        String key = normalizeType(type);
        Objects.requireNonNull(date, "date");
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            ensureTypeExists(key);
            int base = baseCounts.get(key);
            int booked = bookedByTypeAndDate.getOrDefault(key, Collections.emptyMap())
                    .getOrDefault(date, 0);
            int available = base - booked;
            return Math.max(available, 0);
        }
    }

    public int getAvailableForRange(String type, LocalDate startInclusive, LocalDate endExclusive) {
        String key = normalizeType(type);
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must be after startInclusive");
        }
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            ensureTypeExists(key);
            int minAvail = Integer.MAX_VALUE;
            for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) {
                int avail = getAvailableOnDateLocked(key, d); // call locked helper to avoid re-lock
                if (avail < minAvail) minAvail = avail;
                if (minAvail == 0) break;
            }
            return minAvail == Integer.MAX_VALUE ? 0 : minAvail;
        }
    }

    public boolean reserveDates(String type, LocalDate startInclusive, LocalDate endExclusive) {
        String key = normalizeType(type);
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must be after startInclusive");
        }
        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            ensureTypeExists(key);

            for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) {
                int base = baseCounts.get(key);
                int booked = bookedByTypeAndDate.get(key).getOrDefault(d, 0);
                if (booked >= base) {
                    return false; 
                }
            }
            Map<LocalDate, Integer> calendar = bookedByTypeAndDate.computeIfAbsent(key, k -> new HashMap<>());
            for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) {
                int cur = calendar.getOrDefault(d, 0);
                calendar.put(d, cur + 1);
            }
            return true;
        }
    }

    public void releaseDates(String type, LocalDate startInclusive, LocalDate endExclusive) {
        String key = normalizeType(type);
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must be after startInclusive");
        }
        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            ensureTypeExists(key);
            Map<LocalDate, Integer> calendar = bookedByTypeAndDate.get(key);
            if (calendar == null) return;
            for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) {
                Integer cur = calendar.get(d);
                if (cur != null && cur > 0) {
                    int next = cur - 1;
                    if (next == 0) {
                        calendar.remove(d);
                    } else {
                        calendar.put(d, next);
                    }
                }
            }
        }
    }

    // ---------------- Helpers ----------------

    private static String normalizeType(String type) {
        Objects.requireNonNull(type, "room type cannot be null");
        String t = type.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("room type cannot be blank");
        return t;
    }

    private static void validateNonNegativeCount(int count) {
        if (count < 0) throw new IllegalArgumentException("count cannot be negative");
    }

    private static void validateNonNegativePrice(double price) {
        if (price < 0.0d) throw new IllegalArgumentException("price cannot be negative");
    }

    private void ensureTypeExists(String key) {
        if (!baseCounts.containsKey(key) || !pricesPerNight.containsKey(key)) {
            throw new IllegalArgumentException("Unknown room type: " + key);
        }
    }

    private int getMaxBookedLocked(String key) {
        Map<LocalDate, Integer> calendar = bookedByTypeAndDate.get(key);
        int max = 0;
        if (calendar != null) {
            for (int v : calendar.values()) {
                if (v > max) max = v;
            }
        }
        return max;
    }

    private int getAvailableOnDateLocked(String key, LocalDate date) {
        int base = baseCounts.get(key);
        int booked = bookedByTypeAndDate.getOrDefault(key, Collections.emptyMap())
                .getOrDefault(date, 0);
        return Math.max(base - booked, 0);
    }
}