package com.kaua.template.domain.utils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ULID implements Serializable, Comparable<ULID> {

    // Encoded in Crockford's Base32 (0-9A-HJ-NP-Za-km-z)
    public static final int STRING_LENGTH = 26;

    // Binary representation length
    public static final int BINARY_LENGTH = 16;

    // Entropy length (80 bits) final
    public static final int ENTROPY_LENGTH = 10;

    // Minimum allowed timestamp value
    public static final long MIN_TIME = 0x0L;

    /**
     * Maximum allowed timestamp value. Encoded value can encode up to 0x0003ffffffffffffL but ULID
     * binary/byte representation states that timestamp will only be 48-bits.
     */
    public static final long MAX_TIME = 0x0000ffffffffffffL;

    // Crockford's Base32 encoding
    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    // Lookup table for Base32 decoding
    private static final byte[] DECODING = new byte[128];
    static {
        Arrays.fill(DECODING, (byte) -1);
        for (int i = 0; i < ENCODING.length; i++) {
            DECODING[ENCODING[i]] = (byte) i;
        }
    }

    private static final int ASCII_z_INDEX = 122;
    private static final int MASK_16_BITS = 0xFFFF;

    private final long msb; // Most significant 64 bits (timestamp + part of entropy)
    private final long lsb; // Least significant 64 bits (part of entropy)
    private String value; // Cached string representation

    public ULID(final long msb, final long lsb) {
        this.msb = msb;
        this.lsb = lsb;
        this.value = toString();
    }

    // Returns most significant 64 bits of 128bit binary representation.
    public long getMsb() {
        return msb;
    }

    // Returns least significant 64 bits of 128bit binary representation.
    public long getLsb() {
        return lsb;
    }

    @Override
    public int compareTo(final ULID ulid) {
        return this.msb < ulid.msb ? -1 :
                (this.msb > ulid.msb ? 1 :
                        (Long.compare(this.lsb, ulid.lsb)));
    }

    @Override
    public int hashCode() {
        long hilo = msb ^ lsb;
        return ((int) (hilo >> 32)) ^ (int) hilo;
    }

    @Override
    public boolean equals(final Object obj) {
        if ((null == obj) || (obj.getClass() != ULID.class)) return false;
        ULID other = (ULID)obj;
        return msb == other.msb && lsb == other.lsb;
    }

    @Override
    public String toString() {
        if (this.value == null) {
            char[] chars = new char[STRING_LENGTH];

            encodeBase32(msb >>> 16, 10, chars, 0);
            encodeBase32((msb & 0xFFFFL) << 24 | (lsb >>> 40 & 0xFFFFFF), 8, chars, 10);
            encodeBase32(lsb & 0xFFFFFFFFFFL, 8, chars, 18);
            this.value = new String(chars);
        }

        return this.value;
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[BINARY_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putLong(msb);
        buffer.putLong(lsb);
        return bytes;
    }

    // Returns the timestamp part of the ULID as a long.
    public long getTimestamp() {
        return msb >>> 16;
    }

    // Returns the entropy part of the ULID as a byte array.
    public byte[] getEntropy() {
        return new byte[]{
                (byte) (msb >> 8 & MASK_16_BITS),
                (byte) (msb & MASK_16_BITS),
                (byte) (lsb >> 56 & MASK_16_BITS),
                (byte) (lsb >> 48 & MASK_16_BITS),
                (byte) (lsb >> 40 & MASK_16_BITS),
                (byte) (lsb >> 32 & MASK_16_BITS),
                (byte) (lsb >> 24 & MASK_16_BITS),
                (byte) (lsb >> 16 & MASK_16_BITS),
                (byte) (lsb >> 8 & MASK_16_BITS),
                (byte) (lsb & MASK_16_BITS)
        };
    }

    // Generates ULID from current time and random generator
    public static ULID random() {
        return random(ThreadLocalRandom.current());
    }

    /**
     * Generates random ULID with custom random generator
     * <p>
     * Example:
     * <pre>
     *     ULID.random(ThreadLocalRandom.current());
     * </pre>
     */
    public static ULID random(final Random random) {
        byte[] entropy = new byte[ENTROPY_LENGTH];
        random.nextBytes(entropy);
        return noCheckGenerate(Instant.now().toEpochMilli(), entropy);
    }

    /**
     * Generates ULID from raw timestamp and entropy
     * @param time 48-bit timestamp
     * @param entropy 80-bit random data
     */
    public static ULID generate(final long time, final byte[] entropy) {
        if (time < MIN_TIME || time > MAX_TIME) {
            throw new IllegalArgumentException("Invalid timestamp min or max");
        }
        if (entropy == null || entropy.length != ENTROPY_LENGTH) {
            throw new IllegalArgumentException("Invalid entropy");
        }
        return noCheckGenerate(time, entropy);
    }

    /**
     * Parse ULID from string representation
     * @param value 26-character string of Crockford Base32
     */
    public static ULID fromString(final String value) {
        if (value.length() != STRING_LENGTH) {
            throw new IllegalArgumentException("Invalid ULID string length");
        }
        // validate and preprocess chars
        byte[] in = new byte[STRING_LENGTH];
        for (int i = 0; i < STRING_LENGTH; i++) {
            in[i] = validateUlid(value.charAt(i));
        }

        // Timestamp
        long msb = (long) ((in[0] << 5) | in[1]) << 56
                | (long) ((in[2]<< 3) | (in[3] & MASK_16_BITS) >>> 2) << 48
                | (long) ((in[3]<< 6) | in[4]<< 1 | (in[5]& MASK_16_BITS) >>> 4) << 40
                | (long) ((in[5]<< 4) | (in[6]& MASK_16_BITS) >>> 1) << 32
                | (long) ((in[6]<< 7) | in[7]<< 2 | (in[8]& MASK_16_BITS) >>> 3) << 24
                | (long) ((in[8]<< 5)  | in[9]) << 16
                // Entropy
                | (long) ((in[10]<< 3) | (in[11]& MASK_16_BITS) >>> 2) << 8
                | ((in[11]<< 6) | in[12]<< 1 | (in[13]& MASK_16_BITS) >>> 4);

        long lsb = (long) ((in[13]<< 4) | (in[14]& MASK_16_BITS) >>> 1) << 56
                | (long) ((in[14]<< 7) | in[15]<< 2 | (in[16]& MASK_16_BITS) >>> 3) << 48
                | (long) ((in[16]<< 5) | in[17]) << 40
                | (long) ((in[18]<< 3) | (in[19]& MASK_16_BITS) >>> 2) << 32
                | (long) ((in[19]<< 6) | in[20]<< 1 | (in[21]& MASK_16_BITS) >>> 4) << 24
                | (long) ((in[21]<< 4) | (in[22]& MASK_16_BITS) >>> 1) << 16
                | (long) ((in[22]<< 7) | in[23]<< 2 | (in[24]& MASK_16_BITS) >>> 3) << 8
                | ((in[24]<< 5) | in[25]);

        return new ULID(msb, lsb);
    }

    public static ULID fromBytes(final byte[] bytes) {
        if (bytes == null || bytes.length != BINARY_LENGTH) {
            throw new IllegalArgumentException("Invalid byte array length: " + (bytes == null ? 0 : bytes.length));
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();
        return new ULID(mostSignificantBits, leastSignificantBits);
    }

    private static ULID noCheckGenerate(final long time, final byte[] entropy) {
        long msb = time << 16 | ((entropy[0] & MASK_16_BITS) << 8) | (entropy[1] & MASK_16_BITS);
        long lsb = bytesToLong(entropy, 2);
        return new ULID(msb, lsb);
    }

    public static byte validateUlid(final char c) {
        byte aResult;
        if (c > ASCII_z_INDEX || (aResult = DECODING[c]) == (byte)MASK_16_BITS) {
            throw new IllegalArgumentException("Invalid ULID character: " + c);
        }
        return aResult;
    }

    private static long bytesToLong(final byte[] src, final int offset) {
        return ((long) src[offset] & MASK_16_BITS) << 56
                | ((long) src[offset + 1] & MASK_16_BITS) << 48
                | ((long) src[offset + 2] & MASK_16_BITS) << 40
                | ((long) src[offset + 3] & MASK_16_BITS) << 32
                | ((long) src[offset + 4] & MASK_16_BITS) << 24
                | ((long) src[offset + 5] & MASK_16_BITS) << 16
                | ((long) src[offset + 6] & MASK_16_BITS) << 8
                | ((long) src[offset + 7] & MASK_16_BITS);
    }

    /**
     * Encodes the given value into Base32 and stores the result in the destination array.
     *
     * @param value  the value to encode
     * @param length the number of Base32 characters to generate
     * @param dest   the destination character array
     * @param offset the starting position in the destination array
     */
    private void encodeBase32(
            long value,
            int length,
            char[] dest,
            int offset
    ) {
        if (offset + length > dest.length) {
            throw new IllegalArgumentException("Destination array too small, destination length: " + dest.length);
        }

        // Encode the value into Base32
        // 5 bits per character
        for (int i = length - 1; i >= 0; i--) {
            dest[offset + i] = ENCODING[(int) (value & 0x1F)];
            value >>>= 5;
        }
    }
}
