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
