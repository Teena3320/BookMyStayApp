package services;

import model.ReservationRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BookingQueueService {

    private final BlockingQueue<ReservationRequest> queue = new LinkedBlockingQueue<>();

    public void submit(ReservationRequest request) {
        Objects.requireNonNull(request, "request");
        queue.add(request);
    }

    public ReservationRequest peekNext() {
        return queue.peek();
    }

    public ReservationRequest pollNext() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public List<ReservationRequest> snapshotPending() {
        List<ReservationRequest> list = new ArrayList<>(queue);
        return Collections.unmodifiableList(list);
    }
}