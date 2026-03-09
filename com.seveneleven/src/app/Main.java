// src/main/java/com/bookmystay/app/Main.java
package app;

import model.InventorySnapshot;
import model.Reservation;
import model.ReservationRequest;
import model.RoomView;
import model.ServiceItem;
import services.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Use Case 5: Add‑On Service Selection
 * - Allows guests to enhance their confirmed reservation with optional services 
 *   such as breakfast, airport pickup, spa, extra bed, etc.
 * - Maintains a one‑to‑many relationship: one reservation can have multiple services.
 * - Uses a centralized Service Catalog to provide valid service codes, names, and pricing.
 * - Stores services per reservation in a thread‑safe map (reservationId → List<ServiceItem>).
 * - Computes total additional charges for all attached services for billing and reporting.
 * - Ensures immutability of ServiceItem objects and provides read‑only snapshots for safety.
 * - Integrates with UC4 by requiring a valid Reservation ID (only confirmed bookings can add services).
 */

public class Main {

    private static final Scanner SC = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        InventoryService inventory = new InventoryService();
        SearchService search = new SearchService(inventory);
        BookingQueueService bookingQueue = new BookingQueueService();
        BookingService bookingService = new BookingService();
        ServiceCatalog catalog = new ServiceCatalog();
        ServiceManagement services = new ServiceManagement();
        seedDefaults(inventory);

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option (1-18): ");
            switch (choice) {
                case 1 -> addRoomType(inventory);
                case 2 -> updateBaseCount(inventory);
                case 3 -> updatePrice(inventory);
                case 4 -> viewBaseCounts(inventory);
                case 5 -> viewPrices(inventory);
                case 6 -> viewSnapshot(inventory);
                case 7 -> getAvailabilityOnDate(inventory);
                case 8 -> getPrice(inventory);
                case 9 -> listAvailableRoomsForRange(search);
                case 10 -> submitBookingRequest(inventory, bookingQueue);
                case 11 -> viewPendingRequests(bookingQueue);
                case 12 -> confirmNextBooking(bookingQueue, bookingService, inventory);
                case 13 -> viewConfirmedReservations(bookingService);
                case 14 -> listServiceCatalog(catalog);
                case 15 -> addServiceToReservation(bookingService, catalog, services);
                case 16 -> viewServicesForReservation(bookingService, services);
                case 17 -> viewReservationBill(bookingService, services);
                case 18 -> running = false;
                default -> System.out.println("Invalid option. Please choose between 1 and 18.");
            }
            if (running) {
                System.out.println("\nPress ENTER to continue...");
                SC.nextLine();
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n=== BookMyStay (UC1–UC5) ===");
        System.out.println("1)  Add Room Type");
        System.out.println("2)  Update Base Count (±delta)");
        System.out.println("3)  Update Price");
        System.out.println("4)  View Base Counts");
        System.out.println("5)  View Prices");
        System.out.println("6)  View Base Snapshot");
        System.out.println("7)  Get Availability by Room Type on a Date");
        System.out.println("8)  Get Price by Room Type");
        System.out.println("9)  Search Available Rooms for Date Range");
        System.out.println("10) Submit Booking Request ");
        System.out.println("11) View Pending Booking Requests");
        System.out.println("12) Confirm Next Booking ");
        System.out.println("13) View Confirmed Reservations  ");
        System.out.println("14) List Service Catalog ");
        System.out.println("15) Add Service to a Reservation ");
        System.out.println("16) View Services for a Reservation ");
        System.out.println("17) View Reservation Bill (Room + Services)");
        System.out.println("18) Exit");
    }

    // ---------- UC5 actions ----------

    private static void listServiceCatalog(ServiceCatalog catalog) {
        System.out.println("\n-- Service Catalog --");
        var codeToName = catalog.snapshotCodeToName();
        var codeToPrice = catalog.snapshotCodeToPrice();

        if (codeToName.isEmpty()) {
            System.out.println("  (no services available)");
            return;
        }

        // Full table with CODE visible explicitly
        System.out.printf("%-20s | %-28s | %-10s%n", "Code", "Name", "Unit Price");
        System.out.println("----------------------+------------------------------+------------");
        for (String code : codeToName.keySet()) {
            String name = codeToName.get(code);
            double price = codeToPrice.getOrDefault(code, 0.0);
            System.out.printf("%-20s | %-28s | %-10.2f%n", code, name, price);
        }

        // Copy-friendly line of codes for quick reference
        System.out.print("\nCopyable Service Codes: ");
        boolean first = true;
        for (String code : codeToName.keySet()) {
            if (!first) System.out.print(", ");
            System.out.print(code);
            first = false;
        }
        System.out.println();
    }

    private static void addServiceToReservation(BookingService bookingService, ServiceCatalog catalog, ServiceManagement services) {
        String reservationId = readString("Enter Reservation ID (see option 13 to list IDs): ");
        Optional<Reservation> maybeRes = bookingService.findConfirmedById(reservationId);
        if (maybeRes.isEmpty()) {
            System.out.println("Reservation not found. Confirm a booking first.");
            return;
        }

        listServiceCatalog(catalog);
        String code = readString("Enter Service Code (exactly as listed): ").toUpperCase();
        int qty = readInt("Enter quantity (> 0): ");

        var itemOpt = catalog.createItem(code, qty);
        if (itemOpt.isEmpty()) {
            System.out.println("Invalid service code or quantity.");
            return;
        }
        ServiceItem item = itemOpt.get();
        services.addService(reservationId, item);
        System.out.printf("Added service '%s' x %d to Reservation %s. Line total: %.2f%n",
                item.getDisplayName(), item.getQuantity(), reservationId, item.getSubtotal());
    }

    private static void viewServicesForReservation(BookingService bookingService, ServiceManagement services) {
        String reservationId = readString("Enter Reservation ID (see option 13 to list IDs): ");
        Optional<Reservation> maybeRes = bookingService.findConfirmedById(reservationId);
        if (maybeRes.isEmpty()) {
            System.out.println("Reservation not found.");
            return;
        }
        var list = services.getServices(reservationId);
        System.out.println("\n-- Services for Reservation " + reservationId + " --");
        if (list.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        System.out.printf("%-20s | %-10s | %-8s | %-10s%n", "Service", "UnitPrice", "Qty", "Subtotal");
        System.out.println("----------------------+------------+----------+------------");
        for (ServiceItem s : list) {
            System.out.printf("%-20s | %-10.2f | %-8d | %-10.2f%n",
                    s.getDisplayName(), s.getUnitPrice(), s.getQuantity(), s.getSubtotal());
        }
        double total = services.computeServiceTotal(reservationId);
        System.out.printf("Add-on Services Total: %.2f%n", total);
    }

    private static void viewReservationBill(BookingService bookingService, ServiceManagement services) {
        String reservationId = readString("Enter Reservation ID (see option 13 to list IDs): ");
        Optional<Reservation> maybeRes = bookingService.findConfirmedById(reservationId);
        if (maybeRes.isEmpty()) {
            System.out.println("Reservation not found.");
            return;
        }
        Reservation r = maybeRes.get();
        double addons = services.computeServiceTotal(reservationId);
        double grand = r.getTotalCost() + addons;

        System.out.println("\n-- Reservation Bill --");
        System.out.printf("Reservation ID : %s%n", r.getReservationId());
        System.out.printf("Guest          : %s%n", r.getGuestIdOrName());
        System.out.printf("Room Type      : %s%n", r.getRoomType());
        System.out.printf("Room Charge    : %.2f (%d nights @ %.2f)%n",
                r.getTotalCost(), r.getNights(), r.getPricePerNight());
        System.out.printf("Add-ons Total  : %.2f%n", addons);
        System.out.printf("Grand Total    : %.2f%n", grand);
    }

    // ---------- UC4 actions ----------

    private static void confirmNextBooking(BookingQueueService queue, BookingService bookingService, InventoryService inventory) {
        Reservation reservation = bookingService.confirmNext(queue, inventory);
        if (reservation == null) {
            if (queue.size() == 0) {
                System.out.println("No pending requests to process.");
            } else {
                System.out.println("Head request cannot be confirmed now (insufficient availability on some dates). Try later.");
            }
            return;
        }
        switch (reservation.getStatus()) {
            case CONFIRMED -> {
                System.out.println("\n-- Reservation Confirmed --");
                System.out.printf("ReservationID : %s%n", reservation.getReservationId());
                System.out.printf("RequestID     : %s%n", reservation.getRequestId());
                System.out.printf("Guest         : %s%n", reservation.getGuestIdOrName());
                System.out.printf("RoomType      : %s%n", reservation.getRoomType());
                System.out.printf("RoomID        : %s%n", reservation.getRoomId());
                System.out.printf("CheckIn       : %s%n", reservation.getCheckIn());
                System.out.printf("CheckOut      : %s%n", reservation.getCheckOut());
                System.out.printf("Nights        : %d%n", reservation.getNights());
                System.out.printf("Price/Night   : %.2f%n", reservation.getPricePerNight());
                System.out.printf("Total         : %.2f%n", reservation.getTotalCost());
                System.out.println("\nNOTE: Use this Reservation ID to add services (menu 15) or view bill (menu 17).");
            }
            case REJECTED -> {
                System.out.println("\n-- Reservation Rejected --");
                System.out.printf("RequestID     : %s%n", reservation.getRequestId());
                System.out.printf("Guest         : %s%n", reservation.getGuestIdOrName());
                System.out.printf("RoomType      : %s%n", reservation.getRoomType());
                System.out.println("Reason        : See RoomID note -> " + reservation.getRoomId());
            }
            default -> System.out.println("Unhandled status: " + reservation.getStatus());
        }
    }

    private static void viewConfirmedReservations(BookingService bookingService) {
        List<Reservation> list = bookingService.listConfirmed();
        System.out.println("\n-- Confirmed Reservations --");
        if (list.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        System.out.printf("%-12s | %-12s | %-15s | %-10s | %-10s | %-8s | %-10s | %-10s%n",
                "ReservationID", "RequestID", "Guest", "RoomType", "RoomID", "Nights", "Price/N", "Total");
        System.out.println("------------+--------------+-----------------+------------+------------+----------+------------+------------");
        for (Reservation r : list) {
            System.out.printf("%-12s | %-12s | %-15s | %-10s | %-10s | %-8d | %-10.2f | %-10.2f%n",
                    r.getReservationId(),
                    r.getRequestId(),
                    truncate(r.getGuestIdOrName(), 15),
                    r.getRoomType(),
                    r.getRoomId(),
                    r.getNights(),
                    r.getPricePerNight(),
                    r.getTotalCost());
        }
        System.out.print("\nReservation IDs: ");
        for (int i = 0; i < list.size(); i++) {
            System.out.print(list.get(i).getReservationId());
            if (i < list.size() - 1) System.out.print(", ");
        }
        System.out.println();
    }

    // ---------- UC3 actions ----------

    private static void submitBookingRequest(InventoryService inventory, BookingQueueService bookingQueue) {
        String guest = readString("Enter guest name or ID: ");
        String roomType = readString("Enter desired room type (e.g., Single): ");
        if (!inventory.hasRoomType(roomType)) {
            System.out.println("Error: Unknown room type '" + roomType + "'. Please add it first (option 1).");
            return;
        }
        LocalDate checkIn = readDate("Enter check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate("Enter check-out date (YYYY-MM-DD): ");
        try {
            ReservationRequest req = new ReservationRequest.Builder()
                    .guestIdOrName(guest)
                    .roomType(roomType)
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .build();
            bookingQueue.submit(req);
            System.out.println("Request submitted. RequestID: " + req.getRequestId());
            System.out.println("NOTE: After confirmation (menu 12), use menu 13 to view your Reservation ID.");
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void viewPendingRequests(BookingQueueService bookingQueue) {
        var pending = bookingQueue.snapshotPending();
        System.out.println("\n-- Pending Booking Requests (FIFO) --");
        if (pending.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        System.out.printf("%-12s | %-15s | %-10s | %-10s | %-24s%n",
                "RequestID", "Guest", "RoomType", "CheckIn", "SubmittedAt");
        System.out.println("------------+-----------------+------------+------------+--------------------------");
        for (var r : pending) {
            System.out.printf("%-12s | %-15s | %-10s | %-10s | %-24s%n",
                    r.getRequestId(),
                    truncate(r.getGuestIdOrName(), 15),
                    r.getRoomType(),
                    r.getCheckIn(),
                    r.getSubmittedAt());
        }
        System.out.println("Total pending: " + bookingQueue.size());
    }

    // ---------- UC2 actions ----------

    private static void listAvailableRoomsForRange(SearchService search) {
        LocalDate start = readDate("Enter start date (YYYY-MM-DD): ");
        LocalDate end = readDate("Enter end date (YYYY-MM-DD): ");
        if (!end.isAfter(start)) {
            System.out.println("End date must be after start date.");
            return;
        }
        System.out.println("\n-- Available Rooms for Range " + start + " to " + end + " --");
        List<RoomView> views = search.listAvailableRooms(start, end);
        if (views.isEmpty()) {
            System.out.println("  No room types available for the entire range.");
            return;
        }
        System.out.printf("  %-12s | %-8s | %-10s%n", "RoomType", "MinAvail", "Price");
        System.out.println("  -------------+----------+------------");
        for (RoomView v : views) {
            System.out.printf("  %-12s | %-8d | %-10.2f%n",
                    v.getRoomType(), v.getAvailableCount(), v.getPricePerNight());
        }
    }

    // ---------- UC1 actions ----------

    private static void addRoomType(InventoryService inventory) {
        String type = readString("Enter room type name (e.g., Single, Double, Suite): ");
        int count = readInt("Enter base count (integer >= 0): ");
        double price = readDouble("Enter price per night (number >= 0): ");
        try {
            inventory.addRoomType(type, count, price);
            System.out.println("Added room type '" + type + "' with base=" + count + ", price=" + price);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void updateBaseCount(InventoryService inventory) {
        String type = readString("Enter room type name to update base (e.g., Single): ");
        int delta = readInt("Enter base count delta (integer; positive to add, negative to reduce): ");
        try {
            inventory.updateCount(type, delta);
            System.out.println("Updated base for '" + type + "'. New base: " + inventory.getAvailableCount(type));
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void updatePrice(InventoryService inventory) {
        String type = readString("Enter room type name to update price (e.g., Double): ");
        double price = readDouble("Enter new price per night (number >= 0): ");
        try {
            inventory.updatePrice(type, price);
            System.out.println("Updated price for '" + type + "' to " + String.format("%.2f", inventory.getPrice(type)));
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void viewBaseCounts(InventoryService inventory) {
        System.out.println("\n-- Base Room Counts (not date-aware) --");
        Map<String, Integer> counts = inventory.snapshotCounts();
        if (counts.isEmpty()) {
            System.out.println("  (no room types found)");
            return;
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.printf("  %-12s : %d%n", e.getKey(), e.getValue());
        }
    }

    private static void viewPrices(InventoryService inventory) {
        System.out.println("\n-- Room Prices --");
        Map<String, Double> prices = inventory.snapshotPrices();
        if (prices.isEmpty()) {
            System.out.println("  (no room types found)");
            return;
        }
        for (Map.Entry<String, Double> e : prices.entrySet()) {
            System.out.printf("  %-12s : %.2f%n", e.getKey(), e.getValue());
        }
    }

    private static void viewSnapshot(InventoryService inventory) {
        InventorySnapshot snapshot = inventory.snapshot();
        System.out.println("\n-- Base Inventory Snapshot --");
        System.out.println("Counts:");
        if (snapshot.getCounts().isEmpty()) {
            System.out.println("  (no room types found)");
        } else {
            for (Map.Entry<String, Integer> e : snapshot.getCounts().entrySet()) {
                System.out.printf("  %-12s : %d%n", e.getKey(), e.getValue());
            }
        }
        System.out.println("Prices:");
        if (snapshot.getPrices().isEmpty()) {
            System.out.println("  (no room types found)");
        } else {
            for (Map.Entry<String, Double> e : snapshot.getPrices().entrySet()) {
                System.out.printf("  %-12s : %.2f%n", e.getKey(), e.getValue());
            }
        }
    }

    private static void getAvailabilityOnDate(InventoryService inventory) {
        String type = readString("Enter room type name (e.g., Suite): ");
        LocalDate date = readDate("Enter date (YYYY-MM-DD): ");
        try {
            int avail = inventory.getAvailableOnDate(type, date);
            System.out.println("Available '" + type + "' on " + date + " : " + avail);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void getPrice(InventoryService inventory) {
        String type = readString("Enter room type name to check price (e.g., Single): ");
        try {
            double price = inventory.getPrice(type);
            System.out.printf("Price per night for '%s': %.2f%n", type, price);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    // ---------- Helpers ----------

    private static String readString(String prompt) {
        System.out.print(prompt);
        String line = SC.nextLine();
        while (line == null || line.trim().isEmpty()) {
            System.out.print("Input cannot be blank. " + prompt);
            line = SC.nextLine();
        }
        return line.trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = SC.nextLine();
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException | InputMismatchException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = SC.nextLine();
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException | InputMismatchException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = SC.nextLine();
            try {
                return LocalDate.parse(line.trim(), DATE_FMT);
            } catch (DateTimeParseException ex) {
                System.out.println("Please enter a valid date in YYYY-MM-DD format.");
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static void seedDefaults(InventoryService inventory) {
        try { inventory.addRoomType("Single", 10, 2999.00); } catch (IllegalArgumentException ignored) {}
        try { inventory.addRoomType("Double", 8, 4599.50); } catch (IllegalArgumentException ignored) {}
        try { inventory.addRoomType("Suite", 3, 11999.00); } catch (IllegalArgumentException ignored) {}
    }
}