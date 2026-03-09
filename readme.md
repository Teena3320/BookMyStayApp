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
