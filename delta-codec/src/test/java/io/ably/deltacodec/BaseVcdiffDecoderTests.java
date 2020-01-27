package io.ably.deltacodec;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public abstract class BaseVcdiffDecoderTests {
    protected final String stringBase = "Lorem ipsum dolor sit amet";
    protected final String base64Base = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQ=";
    protected final String base64Delta = "1sPEAAABGgAoOAAeBAEsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4TGgEeAA==";
    protected final String base64SecondDelta = "1sPEAAABOABFcwA7BAEgRnVzY2UgaWQgbnVsbGEgbGFjaW5pYSwgdm9sdXRwYXQgb2RpbyB1dCwgdWx0cmljZXMgbGlndWxhLhM4ATsA";
    protected final byte[] base = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116 };
    protected final byte[] delta = new byte[] { (byte)214, (byte)195, (byte)196, 0, 0, 1, 26, 0, 40, 56, 0, 30, 4, 1, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46, 19, 26, 1, 30, 0 };
    protected final byte[] expectedResult = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46 };
    protected final byte[] secondDelta = new byte[] { (byte)214, (byte)195, (byte)196, 0, 0, 1, 56, 0, 69, 115, 0, 59, 4, 1, 32, 70, 117, 115, 99, 101, 32, 105, 100, 32, 110, 117, 108, 108, 97, 32, 108, 97, 99, 105, 110, 105, 97, 44, 32, 118, 111, 108, 117, 116, 112, 97, 116, 32, 111, 100, 105, 111, 32, 117, 116, 44, 32, 117, 108, 116, 114, 105, 99, 101, 115, 32, 108, 105, 103, 117, 108, 97, 46, 19, 56, 1, 59, 0 };
    protected final byte[] secondExpectedResult = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46, 32, 70, 117, 115, 99, 101, 32, 105, 100, 32, 110, 117, 108, 108, 97, 32, 108, 97, 99, 105, 110, 105, 97, 44, 32, 118, 111, 108, 117, 116, 112, 97, 116, 32, 111, 100, 105, 111, 32, 117, 116, 44, 32, 117, 108, 116, 114, 105, 99, 101, 115, 32, 108, 105, 103, 117, 108, 97, 46 };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected abstract DeltaApplicationResult applyDelta(byte[] delta) throws IOException, SequenceContinuityException;
    protected abstract DeltaApplicationResult applyBase64Delta(String delta) throws IOException, SequenceContinuityException;
    protected abstract void setBase(byte[] newBase);
    protected abstract void setBase(String newBase);
    protected abstract void setBase64Base(String newBase);

    @Test
    public void applyDeltaThrowsIllegalStateExceptionWhenBaseIsNull() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Uninitialized decoder - setBase() should be called first");
        this.applyDelta(null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNull() throws IOException, SequenceContinuityException {
        this.expectApplyDeltaToThrowIllegalArgumentExceptionForDelta(null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta1() throws IOException, SequenceContinuityException {
        this.expectApplyDeltaToThrowIllegalArgumentExceptionForDelta(new byte[] { 1, 2, 3, 4 });
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta2() throws IOException, SequenceContinuityException {
        this.expectApplyDeltaToThrowIllegalArgumentExceptionForDelta(new byte[] { 1 });
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta3() throws IOException, SequenceContinuityException {
        this.expectApplyDeltaToThrowIllegalArgumentExceptionForDelta(new byte[] { (byte)214 });
    }

    private void expectApplyDeltaToThrowIllegalArgumentExceptionForDelta(byte[] delta) throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.setBase("baseContent");
        this.applyDelta(delta);
    }

    @Test
    public void applyDeltaReturnsDeltaResultWhenDeltaIsValid() throws IOException, SequenceContinuityException {
        this.setBase(this.base);
        DeltaApplicationResult result = this.applyDelta(this.delta);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void applyDeltaShouldSetBaseProperlyForChaining() throws IOException, SequenceContinuityException {
        this.setBase(this.base);
        this.applyDelta(this.delta);
        DeltaApplicationResult result = this.applyDelta(this.secondDelta);
        assertNotNull(result);
        assertArrayEquals(this.secondExpectedResult, result.asByteArray());
    }

    @Test
    public void applyBase64DeltaThrowsIllegalStateExceptionWhenBaseIsNull() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Uninitialized decoder - setBase() should be called first");
        this.applyBase64Delta(null);
    }

    @Test
    public void applyBase64DeltaThrowsIllegalArgumentExceptionWhenDeltaIsNull() throws IOException, SequenceContinuityException {
        this.expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta(null);
    }

    @Test
    public void applyBase64DeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotBase64EncodedString() throws IOException, SequenceContinuityException {
        this.expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta("!base64EncodedDeltaContent");
    }

    @Test
    public void applyBase64DeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta1() throws IOException, SequenceContinuityException {
        this.expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta("AQIDBA=="); // 1, 2, 3, 4
    }

    @Test
    public void applyBase64DeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta2() throws IOException, SequenceContinuityException {
        this.expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta("AQ=="); // 1
    }

    @Test
    public void applyBase64DeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotVcdiffDelta3() throws IOException, SequenceContinuityException {
        this.expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta("1g=="); // 214 (0xd6)
    }

    private void expectApplyBase64DeltaToThrowIllegalArgumentExceptionForDelta(String delta) throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.setBase("baseContent");
        this.applyBase64Delta(delta);
    }

    @Test
    public void applyBase64DeltaReturnsDeltaResultWhenDeltaIsValid() throws IOException, SequenceContinuityException {
        this.setBase(this.base);
        DeltaApplicationResult result = this.applyBase64Delta(this.base64Delta);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void applyBase64DeltaShouldSetBaseProperlyForChaining() throws IOException, SequenceContinuityException {
        this.setBase(this.base);
        this.applyBase64Delta(this.base64Delta);
        DeltaApplicationResult result = this.applyBase64Delta(this.base64SecondDelta);
        assertNotNull(result);
        assertArrayEquals(this.secondExpectedResult, result.asByteArray());
    }

    @Test
    public void setBaseByteArrayThrowsIllegalArgumentExceptionWhenNewBaseIsNull() {
        thrown.expectMessage("newBase cannot be null");
        thrown.expect(IllegalArgumentException.class);
        this.setBase((byte[])null);
    }

    @Test
    public void setBaseStringThrowsIllegalArgumentExceptionWhenNewBaseIsNull() {
        thrown.expectMessage("newBase cannot be null");
        thrown.expect(IllegalArgumentException.class);
        this.setBase((String)null);
    }

    @Test
    public void setBase64BaseThrowsIllegalArgumentExceptionWhenNewBaseIsNull() {
        thrown.expectMessage("newBase cannot be null");
        thrown.expect(IllegalArgumentException.class);
        this.setBase64Base(null);
    }

    @Test
    public void setBaseByteArrayShouldSetBaseProperly() throws IOException, SequenceContinuityException {
        this.setBase(this.base);
        DeltaApplicationResult result = this.applyDelta(this.delta);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void setBaseStringShouldSetBaseProperly() throws IOException, SequenceContinuityException {
        this.setBase(this.stringBase);
        DeltaApplicationResult result = this.applyDelta(this.delta);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void setBase64BaseShouldSetBaseProperly() throws IOException, SequenceContinuityException {
        this.setBase64Base(this.base64Base);
        DeltaApplicationResult result = this.applyDelta(this.delta);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }
}
