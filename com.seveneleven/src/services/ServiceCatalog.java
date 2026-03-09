package services;

import model.ServiceItem;

import java.util.*;

public class ServiceCatalog {

    private static final class ServiceDef {
        final String code;
        final String name;
        final double unitPrice;

        ServiceDef(String code, String name, double unitPrice) {
            this.code = code;
            this.name = name;
            this.unitPrice = unitPrice;
        }
    }

    private final Map<String, ServiceDef> catalog = new LinkedHashMap<>();

    public ServiceCatalog() {
        addDef(new ServiceDef("BREAKFAST", "Breakfast", 499.00));
        addDef(new ServiceDef("AIRPORT_PICKUP", "Airport Pickup (One-way)", 1200.00));
        addDef(new ServiceDef("SPA", "Spa Access", 2000.00));
        addDef(new ServiceDef("EXTRA_BED", "Extra Bed", 1500.00));
        addDef(new ServiceDef("LATE_CHECKOUT", "Late Checkout", 800.00));
    }

    private void addDef(ServiceDef def) {
        catalog.put(def.code, def);
    }

    public List<String> listServiceCodes() {
        return Collections.unmodifiableList(new ArrayList<>(catalog.keySet()));
    }

    /** Returns a read-only snapshot of (code -> display name). */
    public Map<String, String> snapshotCodeToName() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ServiceDef d : catalog.values()) {
            map.put(d.code, d.name);
        }
        return Collections.unmodifiableMap(map);
    }

    public Map<String, Double> snapshotCodeToPrice() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (ServiceDef d : catalog.values()) {
            map.put(d.code, d.unitPrice);
        }
        return Collections.unmodifiableMap(map);
    }

    public Optional<ServiceItem> createItem(String code, int quantity) {
        if (code == null) return Optional.empty();
        ServiceDef def = catalog.get(code.trim().toUpperCase(Locale.ROOT));
        if (def == null) return Optional.empty();
        if (quantity <= 0) return Optional.empty();
        return Optional.of(new ServiceItem(def.code, def.name, def.unitPrice, quantity));
    }

    public Optional<Double> getUnitPrice(String code) {
        if (code == null) return Optional.empty();
        ServiceDef def = catalog.get(code.trim().toUpperCase(Locale.ROOT));
        return def == null ? Optional.empty() : Optional.of(def.unitPrice);
    }

    public Optional<String> getDisplayName(String code) {
        if (code == null) return Optional.empty();
        ServiceDef def = catalog.get(code.trim().toUpperCase(Locale.ROOT));
        return def == null ? Optional.empty() : Optional.of(def.name);
    }
}