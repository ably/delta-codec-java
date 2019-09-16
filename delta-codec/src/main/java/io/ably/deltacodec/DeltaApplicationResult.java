package io.ably.deltacodec;

import java.nio.charset.StandardCharsets;

/**
 * Contains and manages the result of delta application
 * */
public class DeltaApplicationResult {
    private byte[] data;

    DeltaApplicationResult(byte[] data) {
        this.data = data;
    }

    /**
     * Exports the delta application result as byte[]
     * @return byte[] representation of this delta application result
     */
    public byte[] asByteArray() {
        return this.data;
    }

    /**
     * Exports the delta application result as string assuming the bytes
     * in the result represent an UTF-8 encoded string
     * @return The UTF-8 string representation of this delta application result
     */
    public String asUtf8String() {
        return new String(this.data, StandardCharsets.UTF_8);
    }

    /**
     * Exports the delta application result as object assuming the bytes in
     * the result represent an UTF-8 encoded JSON string
     * @return The object representation of this delta application result
     */
    public Object asObject() {
        return JsonHelper.getInstance().deserialize(this.asUtf8String());
    }
}
