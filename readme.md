# Use Case 1 — Room Inventory Setup & Management
## Goal
Initialize and manage the hotel’s master inventory: room types, base room counts, and prices.

## Key Concepts
HashMap lookups (O(1) access)
Encapsulation of inventory state
Read/write locking for thread‑safety
Defensive snapshots for safe read‑only access

## Flow
Admin enters room type, base count, price.
System validates non‑negative inputs and uniqueness.

## InventoryService stores:

room type → base count
room type → price

Admin can update counts/prices or view snapshots.
Inventory remains the single source of truth.



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

# Use Case 3 — Booking Request (FIFO Queue)
## Goal
Capture booking requests fairly, especially under heavy load.

## Key Concepts
FIFO queueing via LinkedBlockingQueue
Immutable ReservationRequest with timestamp
Separation of request vs confirmed reservation

## Flow
Guest enters room type + stay dates.

## System creates a ReservationRequest with:
Request ID
Guest info
Dates
Timestampa

Request is placed into the queue (First‑Come‑First‑Served).
No allocation happens here — only queuing.

# Use Case 4 — Reservation Confirmation & Room Allocation
## Goal
Process queued requests one-by-one and allocate rooms atomically, blocking only the specific dates reserved.

## Key Concepts
Atomic allocation using ReentrantLock
Room ID generation and uniqueness
Date‑based booking calendar:
roomType → { date → bookedCount }
Reservation creation with status (CONFIRMED/REJECTED)

## Flow
System takes the head of queue.
Checks availability for every date in the range.
If available → reserve dates → decrement available slots only for those dates.
Generate unique Room ID (e.g., SIN-0001).
Create immutable Reservation with Reservation ID.
Add to confirmed history.
If no availability → keep request in queue (no starvation).


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


# Use Case 6 — Booking History, Cancellation & Reporting
## Goal
Maintain a full audit trail of bookings, support cancellation, and generate operational reports.

## Key Concepts
Cancellation releases dates back to the availability calendar
Active vs cancelled reservations view
Revenue calculations over a date range
Grouping & filters for analytics

## Flow
User enters Reservation ID to cancel.

## System validates and:
Releases reserved dates
Marks reservation as CANCELLED
Keeps original reservation for history

## ReportingService supports:
List active reservations
List cancelled reservations
Search by guest
Revenue between date ranges
Room-type counts (active/cancelled)
