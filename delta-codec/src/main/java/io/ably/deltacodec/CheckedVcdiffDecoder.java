package io.ably.deltacodec;

import java.io.IOException;
import java.util.Objects;

/**
 * VCDIFF decoder capable of processing continuous sequences of consecutively generated VCDIFFs
 */
public class CheckedVcdiffDecoder extends BaseVcdiffDecoder {
    private String baseId;

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link CheckedVcdiffDecoder#setBase(byte[], String)},
     * {@link CheckedVcdiffDecoder#setBase(String, String)} or {@link CheckedVcdiffDecoder#setBase64Base(String, String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link CheckedVcdiffDecoder#setBase(byte[], String)},
     * {@link CheckedVcdiffDecoder#setBase(String, String)} or {@link CheckedVcdiffDecoder#setBase64Base(String, String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     * @throws SequenceContinuityException The provided {@code baseId} does not match the last preserved sequence ID
     */
    public DeltaApplicationResult applyDelta(byte[] delta, String deltaId, String baseId) throws SequenceContinuityException, IllegalStateException, IllegalArgumentException, IOException {
        this.checkSequenceContinuity(baseId);
        DeltaApplicationResult result = this.applyDeltaInternal(delta);
        this.baseId = deltaId;
        return result;
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link CheckedVcdiffDecoder#setBase(byte[], String)},
     * {@link CheckedVcdiffDecoder#setBase(String, String)} or {@link CheckedVcdiffDecoder#setBase64Base(String, String)}
     * before calling this method for the first time.
     * @param delta The delta to be applied as base64 string
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link CheckedVcdiffDecoder#setBase(byte[], String)},
     * {@link CheckedVcdiffDecoder#setBase(String, String)} or {@link CheckedVcdiffDecoder#setBase64Base(String, String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     * @throws SequenceContinuityException The provided {@code baseId} does not match the last preserved sequence ID
     */
    public DeltaApplicationResult applyBase64Delta(String delta, String deltaId, String baseId) throws SequenceContinuityException, IllegalStateException, IllegalArgumentException, IOException {
        this.checkSequenceContinuity(baseId);
        DeltaApplicationResult result = this.applyBase64DeltaInternal(delta);
        this.baseId = deltaId;
        return result;
    }

    /**
     * Sets the base object used for the next delta application (see {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)}
     * and {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}).
     * @param newBase The byte[] to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)} or
     *                  {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(byte[] newBase, String newBaseId) {
        this.setBaseInternal(newBase);
        this.baseId = newBaseId;
    }

    /**
     * Sets the base object used for the next delta application (see {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)}
     * and {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}).
     * @param newBase The string to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)} or
     *                  {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(String newBase, String newBaseId) {
        this.setBaseInternal(newBase);
        this.baseId = newBaseId;
    }

    /**
     * Sets the base object used for the next delta application (see {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)}
     * and {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}).
     * @param newBase The base64 string to be set as new base
     * @param newBaseId (Optional) The {@code newBase}'s sequence ID, to be used for sequence continuity checking
     *                  when delta is applied using {@link CheckedVcdiffDecoder#applyDelta(byte[], String, String)} or
     *                  {@link CheckedVcdiffDecoder#applyBase64Delta(String, String, String)}
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase64Base(String newBase, String newBaseId) {
        this.setBase64BaseInternal(newBase);
        this.baseId = newBaseId;
    }

    private void checkSequenceContinuity(String baseId) throws SequenceContinuityException {
        if (!Objects.equals(this.baseId, baseId)) {
            throw new SequenceContinuityException(this.baseId, baseId);
        }
    }
}
