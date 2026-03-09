
# Use Case 5 — Add‑On Service Selection
## Goal
Attach optional hotel services to confirmed reservations and compute totals.

## Key Concepts
One-to-many mapping: reservation → list of ServiceItem
Service catalog with canonical codes
Immutable ServiceItem objects
Aggregation of costs for billing

## Flow
Guest provides a Reservation ID.
System displays the service catalog with codes and prices.
Guest selects service code + quantity.
System validates service code and creates a ServiceItem.
Adds item to reservation’s service list.
Computes total add‑on charges for billing.
