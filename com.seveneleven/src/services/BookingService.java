package services;

import model.Reservation;
import model.ReservationRequest;
import model.ReservationStatus;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class BookingService {

    private final Set<String> assignedRoomIds = new HashSet<>();
    private final Map<String, Set<String>> typeToAssignedRooms = new HashMap<>();
    private final Map<String, Integer> typeSequence = new HashMap<>();
    private final List<Reservation> confirmed = new ArrayList<>();

    private final ReentrantLock allocationLock = new ReentrantLock();
    public Reservation confirmNext(BookingQueueService queue, InventoryService inventory) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(inventory, "inventory");

        ReservationRequest head = queue.peekNext();
        if (head == null) return null;

        allocationLock.lock();
        try {
            if (!inventory.hasRoomType(head.getRoomType())) {
                queue.pollNext();
                return buildRejected(head, "UNKNOWN_ROOM_TYPE");
            }

            int minAvail = inventory.getAvailableForRange(head.getRoomType(), head.getCheckIn(), head.getCheckOut());
            if (minAvail <= 0) {
                return null;
            }

            ReservationRequest req = queue.pollNext();
            if (req == null) return null;

            boolean reserved = inventory.reserveDates(req.getRoomType(), req.getCheckIn(), req.getCheckOut());
            if (!reserved) {
                return null;
            }

            // Generate unique room ID
            String roomId = generateUniqueRoomId(req.getRoomType());
            markAssigned(req.getRoomType(), roomId);

            // Pricing snapshot
            double pricePerNight = inventory.getPrice(req.getRoomType());
            int nights = (int) Math.max(1, ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut()));
            double total = pricePerNight * nights;

            Reservation reservation = new Reservation.Builder()
                    .requestId(req.getRequestId())
                    .guestIdOrName(req.getGuestIdOrName())
                    .roomType(req.getRoomType())
                    .roomId(roomId)
                    .checkIn(req.getCheckIn())
                    .checkOut(req.getCheckOut())
                    .nights(nights)
                    .pricePerNight(pricePerNight)
                    .totalCost(total)
                    .status(ReservationStatus.CONFIRMED)
                    .build();

            confirmed.add(reservation);
            return reservation;

        } finally {
            allocationLock.unlock();
        }
    }

    private Reservation buildRejected(ReservationRequest req, String reason) {
        return new Reservation.Builder()
                .requestId(req.getRequestId())
                .guestIdOrName(req.getGuestIdOrName())
                .roomType(req.getRoomType())
                .roomId("N/A-" + reason)
                .checkIn(req.getCheckIn())
                .checkOut(req.getCheckOut())
                .nights((int) Math.max(1, ChronoUnit.DAYS.between(req.getCheckIn(), req.getCheckOut())))
                .pricePerNight(0.0)
                .totalCost(0.0)
                .status(ReservationStatus.REJECTED)
                .build();
    }

    private String generateUniqueRoomId(String roomType) {
        String norm = roomType.trim().toUpperCase(Locale.ROOT);
        int nextSeq = typeSequence.compute(norm, (k, v) -> (v == null) ? 1 : (v + 1));
        String candidate = norm.substring(0, Math.min(3, norm.length())) + "-" + String.format("%04d", nextSeq);
        while (assignedRoomIds.contains(candidate)) {
            nextSeq = typeSequence.compute(norm, (k, v) -> (v == null) ? 1 : (v + 1));
            candidate = norm.substring(0, Math.min(3, norm.length())) + "-" + String.format("%04d", nextSeq);
        }
        return candidate;
    }

    private void markAssigned(String roomType, String roomId) {
        assignedRoomIds.add(roomId);
        typeToAssignedRooms.computeIfAbsent(roomType, k -> new HashSet<>()).add(roomId);
    }

    public List<Reservation> listConfirmed() {
        return Collections.unmodifiableList(confirmed);
    }

    public boolean isRoomIdAssigned(String roomId) {
        return assignedRoomIds.contains(roomId);
    }
}