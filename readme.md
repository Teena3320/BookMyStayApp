# Use Case 2 — Room Search & Availability Check
## Goal
Allow guests to view available rooms without modifying data.

## Key Concepts
Read‑only operations
Defensive snapshot copies
Date‑aware availability (extended later)
Rendering structured results via RoomView

## Flow
Guest enters desired stay dates.
SearchService checks minimum availability across the entire date range.

## Calculates:
minAvailability = min(baseCount – bookedCount[eachDate])
Returns only those room types with availability > 0.
Display price and available quantity per type.
