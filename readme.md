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
