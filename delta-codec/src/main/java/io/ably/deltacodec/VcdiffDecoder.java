package io.ably.deltacodec;

import java.io.IOException;

/**
 * VCDIFF decoder capable of processing continuous sequences of consecutively generated VCDIFFs
 */
public class VcdiffDecoder extends BaseVcdiffDecoder {
    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(byte[])},
     * {@link VcdiffDecoder#setBase(String)} or {@link VcdiffDecoder#setBase64Base(String)} before calling this
     * method for the first time.
     * @param delta The delta to be applied
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(byte[])},
     * {@link VcdiffDecoder#setBase(String)} or {@link VcdiffDecoder#setBase64Base(String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     */
    public DeltaApplicationResult applyDelta(byte[] delta) throws IllegalStateException, IllegalArgumentException, IOException {
        return this.applyDeltaInternal(delta);
    }

    /**
     * Applies the {@code delta} to the result of applying the previous delta or to the base data
     * if no previous delta has been applied yet. Base data has to be set by {@link VcdiffDecoder#setBase(byte[])},
     * {@link VcdiffDecoder#setBase(String)} or {@link VcdiffDecoder#setBase64Base(String)} before calling this
     * method for the first time.
     * @param delta The delta to be applied as base64 string
     * @return {@link DeltaApplicationResult} instance
     * @throws IOException Delta application failed
     * @throws IllegalStateException The decoder is not initialized by calling {@link VcdiffDecoder#setBase(byte[])},
     * {@link VcdiffDecoder#setBase(String)} or {@link VcdiffDecoder#setBase64Base(String)}
     * @throws IllegalArgumentException The provided {@code delta} is not a valid VCDIFF
     */
    public DeltaApplicationResult applyBase64Delta(String delta) throws IllegalStateException, IllegalArgumentException, IOException {
        return this.applyBase64DeltaInternal(delta);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(byte[])} and
     * {@link VcdiffDecoder#applyBase64Delta(String)}).
     * @param newBase The byte[] to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(byte[] newBase) throws IllegalArgumentException {
        this.setBaseInternal(newBase);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(byte[])} and
     * {@link VcdiffDecoder#applyBase64Delta(String)}).
     * @param newBase The string to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase(String newBase) throws IllegalArgumentException {
        this.setBaseInternal(newBase);
    }

    /**
     * Sets the base object used for the next delta application (see {@link VcdiffDecoder#applyDelta(byte[])} and
     * {@link VcdiffDecoder#applyBase64Delta(String)}).
     * @param newBase The base64 string to be set as new base
     * @throws IllegalArgumentException The provided {@code newBase} parameter is null
     */
    public void setBase64Base(String newBase) throws IllegalArgumentException {
        this.setBase64BaseInternal(newBase);
    }
}
