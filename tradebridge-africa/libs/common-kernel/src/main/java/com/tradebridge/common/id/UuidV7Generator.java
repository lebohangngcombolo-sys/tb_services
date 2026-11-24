package com.tradebridge.common.id;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * RFC 9562 UUID version 7 generator (time-ordered, 48-bit millisecond timestamp).
 * <p>
 * Services should prefer UUID v7 for primary keys so that index locality
 * remains good while identifiers are still globally unique.
 * <p>
 * The implementation is monotonic: within the same millisecond it increments
 * the random "sequence" portion instead of panicking on collisions.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static long lastTimestamp = -1L;
    private static long lastRandom = 0L;

    private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFFFL; // 48 bits
    private static final long VERSION_BITS = 0x7000L;           // version 7
    private static final long RAND_A_MASK = 0x0FFFL;            // 12 bits variant A
    private static final long RAND_B_MASK = 0x3FFF_FFFFFFFFFFFFL;

    private UuidV7Generator() { }

    public static synchronized UUID next() {
        long now = Instant.now().toEpochMilli();

        if (now > lastTimestamp) {
            lastTimestamp = now;
            lastRandom = RANDOM.nextLong() & RAND_B_MASK;
        } else {
            // Same or earlier millisecond — bump the sequence portion monotonically
            lastRandom = (lastRandom + 1) & RAND_B_MASK;
            if (lastRandom == 0) {
                // Extremely unlikely: 2^62 sequence rolls over. Move clock forward.
                lastTimestamp++;
            }
        }

        long ts = lastTimestamp & TIMESTAMP_MASK;
        long randA = RANDOM.nextLong() & RAND_A_MASK;
        long randB = lastRandom;

        long msb = (ts << 16) | VERSION_BITS | randA;
        long lsb = 0x8000_0000_0000_0000L | randB; // variant 10xx

        return new UUID(msb, lsb);
    }
}