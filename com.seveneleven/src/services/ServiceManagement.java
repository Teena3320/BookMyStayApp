package services;

import model.ServiceItem;
import util.ReadWriteLockGuard;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceManagement {

    private final Map<String, List<ServiceItem>> servicesByReservation = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void addService(String reservationId, ServiceItem item) {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(item, "item");
        String key = reservationId.trim();
        if (key.isEmpty()) throw new IllegalArgumentException("reservationId cannot be blank");

        try (ReadWriteLockGuard.Write ignored = ReadWriteLockGuard.acquireWrite(rwLock)) {
            servicesByReservation
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(item);
        }
    }

    public List<ServiceItem> getServices(String reservationId) {
        Objects.requireNonNull(reservationId, "reservationId");
        String key = reservationId.trim();
        if (key.isEmpty()) throw new IllegalArgumentException("reservationId cannot be blank");

        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            List<ServiceItem> list = servicesByReservation.get(key);
            if (list == null) return Collections.emptyList();
            return Collections.unmodifiableList(new ArrayList<>(list));
        }
    }

    public double computeServiceTotal(String reservationId) {
        Objects.requireNonNull(reservationId, "reservationId");
        String key = reservationId.trim();
        if (key.isEmpty()) throw new IllegalArgumentException("reservationId cannot be blank");

        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            List<ServiceItem> list = servicesByReservation.get(key);
            if (list == null) return 0.0;
            double sum = 0.0;
            for (ServiceItem s : list) {
                sum += s.getSubtotal();
            }
            return sum;
        }
    }

    public Map<String, List<ServiceItem>> snapshotAll() {
        try (ReadWriteLockGuard.Read ignored = ReadWriteLockGuard.acquireRead(rwLock)) {
            Map<String, List<ServiceItem>> copy = new HashMap<>();
            for (Map.Entry<String, List<ServiceItem>> e : servicesByReservation.entrySet()) {
                copy.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
            }
            return Collections.unmodifiableMap(copy);
        }
    }
}