package app;

import model.InventorySnapshot;
import model.RoomView;
import model.ReservationRequest;
import services.BookingQueueService;
import services.InventoryService;
import services.SearchService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Use Case 3: Booking Request (First-Come-First-Served)
 * - Captures guest booking intentions without modifying inventory.
 * - Uses a FIFO queue to ensure fair and predictable processing order.
 * - Prevents race conditions during high‑traffic booking scenarios.
 * - Stores each request with timestamp metadata for strict chronological ordering.
 * - Allocation and room assignment are handled later in Use Case 4.
 */

public class Main {

    private static final Scanner SC = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) {
        InventoryService inventory = new InventoryService();
        SearchService search = new SearchService(inventory);
        BookingQueueService bookingQueue = new BookingQueueService();
        seedDefaults(inventory); // optional seeding

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option (1-12): ");
            switch (choice) {
                case 1:
                    addRoomType(inventory);
                    break;
                case 2:
                    updateCount(inventory);
                    break;
                case 3:
                    updatePrice(inventory);
                    break;
                case 4:
                    viewCounts(inventory);
                    break;
                case 5:
                    viewPrices(inventory);
                    break;
                case 6:
                    viewSnapshot(inventory);
                    break;
                case 7:
                    getCount(inventory);
                    break;
                case 8:
                    getPrice(inventory);
                    break;
                case 9:
                    listAvailableRooms(search);
                    break;
                case 10:
                    submitBookingRequest(inventory, bookingQueue);
                    break;
                case 11:
                    viewPendingRequests(bookingQueue);
                    break;
                case 12:
                    running = false; // Exit
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 1 and 12.");
            }
            if (running) {
                System.out.println("\nPress ENTER to continue...");
                SC.nextLine();
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n=== BookMyStay Inventory, Search & Booking Queue ===");
        System.out.println("1)  Add Room Type");
        System.out.println("2)  Update Count (±delta)");
        System.out.println("3)  Update Price");
        System.out.println("4)  View All Counts");
        System.out.println("5)  View All Prices");
        System.out.println("6)  View Full Snapshot");
        System.out.println("7)  Get Count by Room Type");
        System.out.println("8)  Get Price by Room Type");
        System.out.println("9)  Search Available Rooms ");
        System.out.println("10) Submit Booking Request ");
        System.out.println("11) View Pending Booking Requests ");
        System.out.println("12) Exit");
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
            System.out.println("Request submitted in FIFO order. ID: " + req.getRequestId() +
                    " | SubmittedAt: " + req.getSubmittedAt());
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void viewPendingRequests(BookingQueueService bookingQueue) {
        List<ReservationRequest> pending = bookingQueue.snapshotPending();
        System.out.println("\n-- Pending Booking Requests (FIFO) --");
        if (pending.isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        System.out.printf("%-12s | %-15s | %-10s | %-10s | %-24s%n",
                "RequestID", "Guest", "RoomType", "CheckIn", "SubmittedAt");
        System.out.println("------------+-----------------+------------+------------+--------------------------");
        for (ReservationRequest r : pending) {
            System.out.printf("%-12s | %-15s | %-10s | %-10s | %-24s%n",
                    r.getRequestId(),
                    truncate(r.getGuestIdOrName(), 15),
                    r.getRoomType(),
                    r.getCheckIn(),
                    r.getSubmittedAt());
        }
        System.out.println("Total pending: " + bookingQueue.size());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    // --- UC2 actions ---

    private static void listAvailableRooms(SearchService search) {
        System.out.println("\n-- Available Rooms (count > 0) --");
        List<RoomView> views = search.listAvailableRooms();
        if (views.isEmpty()) {
            System.out.println("  No rooms currently available.");
            return;
        }
        System.out.printf("  %-12s | %-6s | %-10s%n", "RoomType", "Count", "Price");
        System.out.println("  -------------+--------+------------");
        for (RoomView v : views) {
            System.out.printf("  %-12s | %-6d | %-10.2f%n",
                    v.getRoomType(), v.getAvailableCount(), v.getPricePerNight());
        }
    }

    // --- UC1 actions ---

    private static void addRoomType(InventoryService inventory) {
        String type = readString("Enter room type name (e.g., Single, Double, Suite): ");
        int count = readInt("Enter initial count (integer >= 0): ");
        double price = readDouble("Enter price per night (number >= 0): ");
        try {
            inventory.addRoomType(type, count, price);
            System.out.println("Added room type '" + type + "' with count=" + count + ", price=" + price);
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void updateCount(InventoryService inventory) {
        String type = readString("Enter room type name to update (e.g., Single): ");
        int delta = readInt("Enter count delta (integer; positive to add, negative to reduce): ");
        try {
            inventory.updateCount(type, delta);
            System.out.println("Updated '" + type + "' by delta " + delta +
                    ". New count: " + inventory.getAvailableCount(type));
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void updatePrice(InventoryService inventory) {
        String type = readString("Enter room type name to update (e.g., Double): ");
        double price = readDouble("Enter new price per night (number >= 0): ");
        try {
            inventory.updatePrice(type, price);
            System.out.println("Updated price for '" + type + "' to " + String.format("%.2f", inventory.getPrice(type)));
        } catch (IllegalArgumentException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    private static void viewCounts(InventoryService inventory) {
        System.out.println("\n-- Room Counts --");
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
        System.out.println("\n-- Inventory Snapshot --");
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

    private static void getCount(InventoryService inventory) {
        String type = readString("Enter room type name to check count (e.g., Suite): ");
        try {
            int count = inventory.getAvailableCount(type);
            System.out.println("Available '" + type + "': " + count);
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

    private static void seedDefaults(InventoryService inventory) {
        try {
            inventory.addRoomType("Single", 10, 2999.00);
        } catch (IllegalArgumentException ignored) {}
        try {
            inventory.addRoomType("Double", 8, 4599.50);
        } catch (IllegalArgumentException ignored) {}
        try {
            inventory.addRoomType("Suite", 3, 11999.00);
        } catch (IllegalArgumentException ignored) {}
    }
}