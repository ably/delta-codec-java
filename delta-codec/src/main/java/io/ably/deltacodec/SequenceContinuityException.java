package io.ably.deltacodec;

/**
 * Thrown when {@link VcdiffDecoder}'s built-in sequence continuity check fails
 */
public class SequenceContinuityException extends Exception {
    SequenceContinuityException(String expectedId, String actualId) {
        super("Sequence continuity check failed - the provided id (" + actualId + ") does not match the last preserved sequence id (" + expectedId + ")");
    }
}
