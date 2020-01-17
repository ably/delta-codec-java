package io.ably.deltacodec;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class CheckedVcdiffDecoderTest {
    private final String base64Base = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQ=";
    private final String base64Delta = "1sPEAAABGgAoOAAeBAEsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4TGgEeAA==";
    private final String base64ExpectedResult = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private final byte[] byteArrayBase = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116 };
    private final byte[] byteArrayDelta = new byte[] { (byte)214, (byte)195, (byte)196, 0, 0, 1, 26, 0, 40, 56, 0, 30, 4, 1, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46, 19, 26, 1, 30, 0 };
    private final byte[] byteArrayExpectedResult = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46 };

    private CheckedVcdiffDecoder checkedDecoder;

    @Before
    public void createVcdiffDecoderInstance() {
        checkedDecoder = new CheckedVcdiffDecoder();
    }

    @After
    public void disposeVcdiffDecoderInstance() {
        checkedDecoder = null;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void setBaseThrowsIllegalArgumentExceptionWhenNewBaseIsNull() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("newBase cannot be null");
        this.checkedDecoder.setBase((byte[])null, null);
    }

    @Test
    public void applyDeltaThrowsIllegalStateExceptionWhenBaseIsNull() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Uninitialized decoder - setBase() should be called first");
        this.checkedDecoder.applyDelta((byte[])null, null, null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotBase64Encoded() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.checkedDecoder.setBase("baseContent", null);
        this.checkedDecoder.applyBase64Delta("!deltaContent", null, null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaDoesNotContainVcdiffHeaderAndIsBase64EncodedArgumentIsFalse() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.checkedDecoder.setBase("baseContent", null);
        this.checkedDecoder.applyDelta(new byte[1], null, null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotBase64EncodedAndIsBase64EncodedArgumentIsTrue() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.checkedDecoder.setBase("baseContent", null);
        this.checkedDecoder.applyBase64Delta("nonBase64Content", null, null);
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaDoesNotContainVcdiffHeaderAndIsBase64EncodedArgumentIsTrue() throws IOException, SequenceContinuityException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The provided delta is not a valid VCDIFF delta");
        this.checkedDecoder.setBase("baseContent", null);
        this.checkedDecoder.applyBase64Delta("YmFzZTY0Q29udGVudA==", null, null);
    }

    @Test
    public void applyBase64DeltaReturnsDeltaResultWhenDeltaIsValid() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase64Base(base64Base, null);
        DeltaApplicationResult deltaResult = this.checkedDecoder.applyBase64Delta(base64Delta, null, null);

        assertEquals(base64ExpectedResult, deltaResult.asUtf8String());
    }

    @Test
    public void applyDeltaReturnsDeltaResultWhenDeltaIsValid() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(byteArrayBase, null);
        DeltaApplicationResult deltaResult = this.checkedDecoder.applyDelta(byteArrayDelta, null, null);

        assertArrayEquals(deltaResult.asByteArray(), byteArrayExpectedResult);
    }

    @Test
    public void applyBase64DeltaThrowsSequenceContinuityExceptionWhenBaseIdsDoesNotMatch() throws IOException, SequenceContinuityException {
        thrown.expect(SequenceContinuityException.class);
        thrown.expectMessage("Sequence continuity check failed - the provided id (3) does not match the last preserved sequence id (1)");
        this.checkedDecoder.setBase64Base(base64Base, "1");
        this.checkedDecoder.applyBase64Delta(base64Delta, "2", "3");
    }

    @Test
    public void applyBase64DeltaDoesNotThrowSequenceContinuityExceptionWhenBaseIdsMatch() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase64Base(base64Base, "1");
        this.checkedDecoder.applyBase64Delta(base64Delta, "2", "1");
    }

    @Test
    public void applyDeltaThrowsSequenceContinuityExceptionWhenBaseIdsDoesNotMatch() throws IOException, SequenceContinuityException {
        thrown.expect(SequenceContinuityException.class);
        thrown.expectMessage("Sequence continuity check failed - the provided id (3) does not match the last preserved sequence id (1)");
        this.checkedDecoder.setBase(byteArrayBase, "1");
        this.checkedDecoder.applyDelta(byteArrayDelta, "2", "3");
    }

    @Test
    public void applyDeltaDoesNotThrowSequenceContinuityExceptionWhenBaseIdsMatch() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(byteArrayBase, "1");
        this.checkedDecoder.applyDelta(byteArrayDelta, "2", "1");
    }
}

