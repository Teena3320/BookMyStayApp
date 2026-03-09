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
