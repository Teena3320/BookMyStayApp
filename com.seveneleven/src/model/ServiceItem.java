package model;

import java.util.Objects;

public final class ServiceItem {
    private final String code;          
    private final String displayName;   
    private final double unitPrice;     
    private final int quantity;         

    public ServiceItem(String code, String displayName, double unitPrice, int quantity) {
        this.code = Objects.requireNonNull(code, "code").trim();
        this.displayName = Objects.requireNonNull(displayName, "displayName").trim();
        if (this.code.isEmpty() || this.displayName.isEmpty()) {
            throw new IllegalArgumentException("code/displayName cannot be blank");
        }
        if (unitPrice < 0.0) {
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }

    @Override
    public String toString() {
        return "ServiceItem{" +
                "code='" + code + '\'' +
                ", displayName='" + displayName + '\'' +
                ", unitPrice=" + unitPrice +
                ", quantity=" + quantity +
                ", subtotal=" + getSubtotal() +
                '}';
    }
}