package app;

import model.InventorySnapshot;
import model.Reservation;
import model.ReservationRequest;
import model.RoomView;
import services.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Use Case 4: Reservation Confirmation & Room Allocation
 * - Dequeues booking requests in strict FIFO order and attempts confirmation.
 * - Performs atomic allocation: generates a unique room ID and reserves the
 *   requested date range in one critical section to prevent double-booking.
 * - Enforces uniqueness via a global Set of assigned room IDs and per-type mapping.
 * - Validates date-range availability (each night in [check-in, check-out)) before confirming.
 * - Updates inventory immediately by marking the specific dates as booked (not the entire base).
 * - Produces an immutable Reservation record with pricing snapshot (price at confirmation),
 *   total nights, and total cost for auditability.
 * - On errors (unknown room type, insufficient availability, or race conditions),
 *   returns a REJECTED reservation or leaves the request queued to avoid head-of-line blocking.
 */

public class Main {

    private static final Scanner SC = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        InventoryService inventory = new InventoryService();
        SearchService search = new SearchService(inventory);
        BookingQueueService bookingQueue = new BookingQueueService();
        BookingService bookingService = new BookingService();
        seedDefaults(inventory);

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option (1-14): ");
            switch (choice) {
                case 1:
                    addRoomType(inventory);
                    break;
                case 2:
                    updateBaseCount(inventory);
                    break;
                case 3:
                    updatePrice(inventory);
                    break;
                case 4:
                    viewBaseCounts(inventory);
                    break;
                case 5:
                    viewPrices(inventory);
                    break;
                case 6:
                    viewSnapshot(inventory);
                    break;
                case 7:
                    getAvailabilityOnDate(inventory);
                    break;
                case 8:
                    getPrice(inventory);
                    break;
                case 9:
                    listAvailableRoomsForRange(search);
                    break;
                case 10:
                    submitBookingRequest(inventory, bookingQueue);
                    break;
                case 11:
                    viewPendingRequests(bookingQueue);
                    break;
                case 12:
                    confirmNextBooking(bookingQueue, bookingService, inventory);
                    break;
                case 13:
                    viewConfirmedReservations(bookingService);
                    break;
                case 14:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 14.");
            }
            if (running) {
                System.out.println("\nPress ENTER to continue...");
                SC.nextLine();
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n=== BookMyStay (Date-based Availability) ===");
        System.out.println("1)  Add Room Type");
        System.out.println("2)  Update Base Count (±delta)");
        System.out.println("3)  Update Price");
        System.out.println("4)  View Base Counts (not date-aware)");
        System.out.println("5)  View Prices");
        System.out.println("6)  View Base Snapshot");
        System.out.println("7)  Get Availability by Room Type on a Date (YYYY-MM-DD)");
        System.out.println("8)  Get Price by Room Type");
        System.out.println("9)  Search Available Rooms for Date Range (YYYY-MM-DD to YYYY-MM-DD)");
        System.out.println("10) Submit Booking Request (FIFO queue)");
        System.out.println("11) View Pending Booking Requests");
        System.out.println("12) Confirm Next Booking (allocates and blocks only requested dates)");
        System.out.println("13) View Confirmed Reservations");
        System.out.println("14) Exit");
    }

    // --- UC4 actions ---

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
    }

    // --- UC3 actions ---

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
            System.out.println("Request submitted. ID: " + req.getRequestId() +
                    " | SubmittedAt: " + req.getSubmittedAt());
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

    // --- UC2 actions (date-aware) ---

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

    // --- UC1 actions (base) ---

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

    // --- Input helpers ---

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