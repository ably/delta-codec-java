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
    private final static VCDiffDecoder decoder = VCDiffDecoderBuilder.builder().buildSimple();
    private byte[] base;
    private String baseId;

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(Object)}
     * before calling this method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(Object)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     */
    public DeltaApplicationResult applyDelta(Object delta) throws IllegalStateException, IllegalArgumentException, IOException {
        if (this.base == null) {
            throw new IllegalStateException("Uninitialized decoder - setBase() should be called first");
        }
        ByteArrayOutputStream decoded = applyDeltaInternal(delta, this.base);
        this.base = decoded.toByteArray();
        // Return a copy to avoid future delta application failures if the returned array is modified
        return new DeltaApplicationResult(decoded.toByteArray());
    }

    private static ByteArrayOutputStream applyDeltaInternal(Object delta, Object previousData) throws IOException {
        byte[] deltaAsByteArray = tryConvertToDeltaByteArray(delta);
        if (deltaAsByteArray == null || !hasVcdiffHeader(deltaAsByteArray)) {
            throw new IllegalArgumentException("The provided delta is not a valid VCDIFF delta");
        }
        byte[] previousDataByteArray = convertToByteArray(previousData);
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        decoder.decode(previousDataByteArray, deltaAsByteArray, decoded);
        return decoded;
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(Object, String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(Object, String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     * @throws SequenceContinuityException The provided {@code baseId} does not match the last preserved sequence ID
     */
    public DeltaApplicationResult applyDelta(Object delta, String deltaId, String baseId) throws SequenceContinuityException, IllegalStateException, IllegalArgumentException, IOException {
        if (!Objects.equals(this.baseId, baseId)) {
            throw new SequenceContinuityException(baseId, this.baseId);
        }
        DeltaApplicationResult result = this.applyDelta(delta);
        this.baseId = deltaId;
        return result;
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(Object)}).
     * @param newBase The base object to be set
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(Object newBase) throws IllegalArgumentException {
        if (newBase == null) {
            throw new IllegalArgumentException("newBase cannot be null");
        }
        
        this.base = convertToByteArray(newBase);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(Object, String, String)}).
     * @param newBase The base object to be set
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link VcdiffDecoder#applyDelta(Object, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(Object newBase, String newBaseId) {
        this.setBase(newBase);
        this.baseId = newBaseId;
    }

    public static byte[] decode (Object delta, Object previousData) throws IOException {
        try {
            return applyDeltaInternal(delta, previousData).toByteArray();
        }
        catch (IOException ioEx) {
            throw ioEx;
        }
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
            byte[] base64DecodeResult = tryConvertFromBase64String(dataAsString);
            if (base64DecodeResult != null) {
                return base64DecodeResult;
            } else {
                return dataAsString.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            return JsonHelper.getInstance().serialize(data).getBytes(StandardCharsets.UTF_8);
        }
    }

    private static byte[] tryConvertToDeltaByteArray(Object obj) {
        if (obj instanceof byte[]) {
            return (byte[])obj;
        } else if (obj instanceof String) {
            return tryConvertFromBase64String((String)obj);
        } else {
            return null;
        }
    }

    private static byte[] tryConvertFromBase64String(String str) {
        try {
            return Base64Coder.decode(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
