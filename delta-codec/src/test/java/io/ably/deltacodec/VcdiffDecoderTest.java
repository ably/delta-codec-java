package io.ably.deltacodec;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;

public class VcdiffDecoderTest {
    private VcdiffDecoder decoder;

    @Before
    public void createVcdiffDecoderInstance() {
        decoder = new VcdiffDecoder();
    }

    @After
    public void disposeVcdiffDecoderInstance() {
        decoder = null;
    }

    @Test
    public void setBaseThrowsIllegalArgumentExceptionWhenNewBaseIsNull() {
        try {
            this.decoder.setBase(null);
        } catch (IllegalArgumentException ex) {
            assertEquals("newBase cannot be null", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaThrowsIllegalStateExceptionWhenBaseIsNull() throws IOException {
        try{
            this.decoder.applyDelta(null);
        } catch (IllegalStateException ex) {
            assertEquals("Uninitialized decoder - setBase() should be called first", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaThrowsIllegalStateExceptionWhenDeltaIsNotInBinaryFormatAndIsBase64EncodedArgumentIsFalse() throws IOException {
        try{
            this.decoder.setBase("baseContent");
            this.decoder.applyDelta("deltaContent");
        } catch (IllegalStateException ex) {
            assertEquals("The provided delta does not represent binary data", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaDoesNotContainVcdiffHeaderAndIsBase64EncodedArgumentIsFalse() throws IOException {
        try{
            this.decoder.setBase("baseContent");
            this.decoder.applyDelta(new byte[1]);
        } catch (IllegalArgumentException ex) {
            assertEquals("The provided delta is not a valid VCDIFF delta", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaIsNotBase64EncodedAndIsBase64EncodedArgumentIsTrue() throws IOException {
        try{
            this.decoder.setBase("baseContent");
            this.decoder.applyDelta("nonBase64Content", true);
        } catch (IllegalArgumentException ex) {
            assertEquals("The provided delta is not a valid VCDIFF delta", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaThrowsIllegalArgumentExceptionWhenDeltaDoesNotContainsVcdiffHeaderAndIsBase64EncodedArgumentIsTrue() throws IOException {
        try{
            this.decoder.setBase("baseContent");
            this.decoder.applyDelta("YmFzZTY0Q29udGVudA==", true);
        } catch (IllegalArgumentException ex) {
            assertEquals("The provided delta is not a valid VCDIFF delta", ex.getMessage());
        }
    }

    @Test
    public void applyDeltaReturnsDeltaResultWhenDeltaIsValidAndIsBase64EncodedArgumentIsTrue() throws IOException {
        String base = "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQ=";
        String delta = "1sPEAAABGgAoOAAeBAEsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4TGgEeAA==";
        String expectedResult = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";

        this.decoder.setBase(base, true);
        DeltaApplicationResult deltaResult = this.decoder.applyDelta(delta, true);

        assertEquals(expectedResult, deltaResult.asUtf8String());
    }

    @Test
    public void applyDeltaReturnsDeltaResultWhenDeltaIsValidAndIsBase64EncodedArgumentIsFalse() throws IOException {
        byte[] base = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116 };
        byte[] delta = new byte[] { (byte)214, (byte)195, (byte)196, 0, 0, 1, 26, 0, 40, 56, 0, 30, 4, 1, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46, 19, 26, 1, 30, 0 };
        byte[] expectedResult = new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46 };

        this.decoder.setBase(base);
        DeltaApplicationResult deltaResult = this.decoder.applyDelta(delta);

        assertArrayEquals(deltaResult.asByteArray(), expectedResult);
    }
}
