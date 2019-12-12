package io.ably.deltacodec;

import com.davidehrmann.vcdiff.VCDiffDecoder;
import com.davidehrmann.vcdiff.VCDiffDecoderBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * VCDIFF decoder capable of processing continuous sequences of consecutively generated VCDIFFs
 */
public class VcdiffDecoder {
    private final VCDiffDecoder decoder = VCDiffDecoderBuilder.builder().buildSimple();
    private byte[] base;
    private String baseId;


    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase64Base(String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied as base64 string
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase64Base(String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     */
    public DeltaApplicationResult applyDelta(String delta) throws IllegalStateException, IllegalArgumentException, IOException {
        if (this.base == null) {
            throw new IllegalStateException("Uninitialized decoder - setBase() should be called first");
        }

        byte[] deltaAsByteArray;

        deltaAsByteArray = tryConvertFromBase64String(delta);

        if(deltaAsByteArray == null) {
            throw new IllegalStateException("The provided delta does not represent binary data");
        }

        if (!hasVcdiffHeader(deltaAsByteArray)) {
            throw new IllegalArgumentException("The provided delta is not a valid VCDIFF delta");
        }

        return new DeltaApplicationResult(applyDeltaInternal(deltaAsByteArray));
    }

    private byte[] applyDeltaInternal(byte[] deltaAsByteArray) throws IOException {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        this.decoder.decode(this.base, deltaAsByteArray, decoded);
        this.base = decoded.toByteArray();
        // Return a copy to avoid future delta application failures if the returned array is modified
        return decoded.toByteArray();
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(byte[])}
     * before calling this method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(byte[])}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     */
    public DeltaApplicationResult applyDelta(byte[] delta) throws IllegalStateException, IllegalArgumentException, IOException {
        if (this.base == null) {
            throw new IllegalStateException("Uninitialized decoder - setBase() should be called first");
        }

        if (delta == null || !hasVcdiffHeader(delta)) {
            throw new IllegalArgumentException("The provided delta is not a valid VCDIFF delta");
        }

        return new DeltaApplicationResult(applyDeltaInternal(delta));
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(String, String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied as base64 string
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(String, String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     * @throws SequenceContinuityException The provided {@code baseId} does not match the last preserved sequence ID
     */
    public DeltaApplicationResult applyDelta(String delta, String deltaId, String baseId) throws SequenceContinuityException, IllegalStateException, IllegalArgumentException, IOException {
        if (!Objects.equals(this.baseId, baseId)) {
            throw new SequenceContinuityException(baseId, this.baseId);
        }
        DeltaApplicationResult result = this.applyDelta(delta);
        this.baseId = deltaId;
        return result;
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(byte[], String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(byte[], String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     * @throws SequenceContinuityException The provided {@code baseId} does not match the last preserved sequence ID
     */
    public DeltaApplicationResult applyDelta(byte[] delta, String deltaId, String baseId) throws SequenceContinuityException, IllegalStateException, IllegalArgumentException, IOException {
        if (!Objects.equals(this.baseId, baseId)) {
            throw new SequenceContinuityException(baseId, this.baseId);
        }
        DeltaApplicationResult result = this.applyDelta(delta);
        this.baseId = deltaId;
        return result;
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(byte[])}).
     * @param newBase The byte[] to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(byte[] newBase) throws IllegalArgumentException {
        if (newBase == null) {
            throw new IllegalArgumentException("newBase cannot be null");
        }
        
        this.base = newBase;
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(String)}).
     * @param newBase The string to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(String newBase) throws IllegalArgumentException {
        if (newBase == null) {
            throw new IllegalArgumentException("newBase cannot be null");
        }

        this.base = convertToByteArray(newBase);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(String)}).
     * @param newBase The base64 string to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase64Base(String newBase) throws IllegalArgumentException {
        if (newBase == null) {
            throw new IllegalArgumentException("newBase cannot be null");
        }

        this.base = convertFromBase64String(newBase);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(String, String, String)}).
     * @param newBase The string to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link VcdiffDecoder#applyDelta(String, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(String newBase, String newBaseId) {
        this.setBase(newBase);
        this.baseId = newBaseId;
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(byte[], String, String)}).
     * @param newBase The byte[] to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link VcdiffDecoder#applyDelta(byte[], String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(byte[] newBase, String newBaseId) {
        this.setBase(newBase);
        this.baseId = newBaseId;
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(String, String, String)}).
     * @param newBase The base64 string to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link VcdiffDecoder#applyDelta(String, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase64Base(String newBase, String newBaseId) {
        this.setBase64Base(newBase);
        this.baseId = newBaseId;
    }

    private static boolean hasVcdiffHeader(byte[] delta) {
        return delta[0] == (byte)0xd6 &&
                delta[1] == (byte)0xc3 &&
                delta[2] == (byte)0xc4 &&
                delta[3] == (byte)0;
    }

    private static byte[] convertToByteArray(Object data) {
        if (data instanceof byte[]) {
            return (byte[])data;
        } else if (data instanceof String) {
            String dataAsString = (String)data;
            return dataAsString.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported data type. Supported types: String, byte[].");
        }
    }

    private static byte[] tryConvertFromBase64String(String str) {
        try {
            return convertFromBase64String(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] convertFromBase64String(String str) {
        return Base64Coder.decode(str);
    }
}
