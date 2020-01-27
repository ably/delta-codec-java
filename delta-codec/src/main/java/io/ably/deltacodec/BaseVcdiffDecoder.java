package io.ably.deltacodec;

import com.davidehrmann.vcdiff.VCDiffDecoder;
import com.davidehrmann.vcdiff.VCDiffDecoderBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

abstract class BaseVcdiffDecoder {
    private final VCDiffDecoder decoder = VCDiffDecoderBuilder.builder().buildSimple();
    private byte[] base;

    protected DeltaApplicationResult applyDeltaInternal(byte[] delta) throws IllegalStateException, IllegalArgumentException, IOException {
        if (this.base == null) {
            throw new IllegalStateException("Uninitialized decoder - setBase() should be called first");
        }

        if (delta == null || !hasVcdiffHeader(delta)) {
            throw new IllegalArgumentException("The provided delta is not a valid VCDIFF delta");
        }

        return new DeltaApplicationResult(doApplyDelta(delta));
    }

    protected DeltaApplicationResult applyBase64DeltaInternal(String delta) throws IllegalStateException, IllegalArgumentException, IOException {
        return this.applyDeltaInternal(tryConvertFromBase64String(delta));
    }

    protected void setBaseInternal(byte[] newBase) throws IllegalArgumentException {
        if (newBase == null) {
            throw new IllegalArgumentException("newBase cannot be null");
        }

        this.base = newBase;
    }

    protected void setBaseInternal(String newBase) throws IllegalArgumentException {
        this.setBaseInternal(tryConvertToByteArray(newBase));
    }

    protected void setBase64BaseInternal(String newBase) throws IllegalArgumentException {
        this.setBaseInternal(tryConvertFromBase64String(newBase));
    }

    private byte[] doApplyDelta(byte[] deltaAsByteArray) throws IOException {
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        this.decoder.decode(this.base, deltaAsByteArray, decoded);
        this.base = decoded.toByteArray();
        // Return a copy to avoid future delta application failures if the returned array is modified
        return decoded.toByteArray();
    }

    private static boolean hasVcdiffHeader(byte[] delta) {
        if (delta.length <= 4) {
            return false;
        }
        return delta[0] == (byte)0xd6 &&
                delta[1] == (byte)0xc3 &&
                delta[2] == (byte)0xc4 &&
                delta[3] == (byte)0;
    }

    private static byte[] tryConvertToByteArray(String str) {
        if (str == null) {
            return null;
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] tryConvertFromBase64String(String str) {
        if (str == null) {
            return null;
        }
        try {
            return Base64Coder.decode(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
