// src/main/java/com/bookmystay/util/ReadWriteLockGuard.java
package util;

import java.util.concurrent.locks.ReadWriteLock;

public final class ReadWriteLockGuard {

    private ReadWriteLockGuard() {}

    public static Read acquireRead(ReadWriteLock lock) {
        lock.readLock().lock();
        return new Read(lock);
    }

    public static Write acquireWrite(ReadWriteLock lock) {
        lock.writeLock().lock();
        return new Write(lock);
    }

    public static final class Read implements AutoCloseable {
        private final ReadWriteLock lock;
        private boolean closed = false;

        private Read(ReadWriteLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                lock.readLock().unlock();
            }
        }
    }

    public static final class Write implements AutoCloseable {
        private final ReadWriteLock lock;
        private boolean closed = false;

        private Write(ReadWriteLock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                lock.writeLock().unlock();
            }
        }
    }
}