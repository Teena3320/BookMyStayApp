package services;

import model.Reservation;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ReportingService {

    private final BookingService bookingService;

    public ReportingService(BookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService, "bookingService");
    }

    public List<Reservation> listActiveReservations() {
        Set<String> cancelledIds = bookingService.cancelledIdsSnapshot();
        return bookingService.listConfirmed()
                .stream()
                .filter(r -> !cancelledIds.contains(r.getReservationId()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Reservation> listCancelledReservations() {
        return bookingService.listCancelled();
    }

    public List<Reservation> byGuestActive(String guestIdOrName) {
        if (guestIdOrName == null || guestIdOrName.trim().isEmpty()) return List.of();
        String key = guestIdOrName.trim();
        return listActiveReservations()
                .stream()
                .filter(r -> key.equalsIgnoreCase(r.getGuestIdOrName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Reservation> byGuestCancelled(String guestIdOrName) {
        if (guestIdOrName == null || guestIdOrName.trim().isEmpty()) return List.of();
        String key = guestIdOrName.trim();
        return listCancelledReservations()
                .stream()
                .filter(r -> key.equalsIgnoreCase(r.getGuestIdOrName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public double revenueBetween(LocalDate fromInclusive, LocalDate toExclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toExclusive, "toExclusive");
        if (!toExclusive.isAfter(fromInclusive)) {
            throw new IllegalArgumentException("toExclusive must be after fromInclusive");
        }
        return listActiveReservations()
                .stream()
                .filter(r -> r.getCheckOut().isAfter(fromInclusive) && r.getCheckIn().isBefore(toExclusive))
                .mapToDouble(Reservation::getTotalCost)
                .sum();
    }

    public Map<String, Long> countActiveByRoomType() {
        return listActiveReservations()
                .stream()
                .collect(Collectors.groupingBy(Reservation::getRoomType, Collectors.counting()));
    }

    public Map<String, Long> countCancelledByRoomType() {
        return listCancelledReservations()
                .stream()
                .collect(Collectors.groupingBy(Reservation::getRoomType, Collectors.counting()));
    }
}