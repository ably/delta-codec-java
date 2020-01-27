package io.ably.deltacodec;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class CheckedVcdiffDecoderTest extends BaseVcdiffDecoderTests {
    private final String baseId = "baseId";
    private final String deltaId = "deltaId";
    private final String secondDeltaId = "secondDeltaId";
    private final String invalidBaseId = "invalidBaseId";

    private CheckedVcdiffDecoder checkedDecoder;

    @Before
    public void createVcdiffDecoderInstance() {
        this.checkedDecoder = new CheckedVcdiffDecoder();
    }

    @After
    public void disposeVcdiffDecoderInstance() {
        this.checkedDecoder = null;
    }

    @Override
    protected DeltaApplicationResult applyDelta(byte[] delta) throws IOException, SequenceContinuityException {
        return this.checkedDecoder.applyDelta(delta, null, null);
    }

    @Override
    protected DeltaApplicationResult applyBase64Delta(String delta) throws IOException, SequenceContinuityException {
        return this.checkedDecoder.applyBase64Delta(delta, null, null);
    }

    @Override
    protected void setBase(byte[] newBase) {
        this.checkedDecoder.setBase(newBase, null);
    }

    @Override
    protected void setBase(String newBase) {
        this.checkedDecoder.setBase(newBase, null);
    }

    @Override
    protected void setBase64Base(String newBase) {
        this.checkedDecoder.setBase64Base(newBase, null);
    }

    @Test
    public void applyDeltaThrowsSequenceContinuityExceptionWhenProvidedBaseIdDoesNotMatchTheOneSetBySetBase() throws IOException, SequenceContinuityException {
        thrown.expect(SequenceContinuityException.class);
        thrown.expectMessage(this.getSequenceContinuityExceptionMessage(this.baseId, this.invalidBaseId));
        this.checkedDecoder.setBase("baseContent", this.baseId);
        this.checkedDecoder.applyDelta(null, null, this.invalidBaseId);
    }

    @Test
    public void applyDeltaShouldSetBaseIdProperlyForChaining() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(this.base, this.baseId);
        this.checkedDecoder.applyDelta(this.delta, this.deltaId, this.baseId);
        DeltaApplicationResult result = this.checkedDecoder.applyDelta(this.secondDelta, this.secondDeltaId, this.deltaId);
        assertNotNull(result);
        assertArrayEquals(this.secondExpectedResult, result.asByteArray());
    }

    @Test
    public void applyBase64DeltaThrowsSequenceContinuityExceptionWhenProvidedBaseIdDoesNotMatchTheOneSetBySetBase() throws IOException, SequenceContinuityException {
        thrown.expect(SequenceContinuityException.class);
        thrown.expectMessage(this.getSequenceContinuityExceptionMessage(this.baseId, this.invalidBaseId));
        this.checkedDecoder.setBase("baseContent", this.baseId);
        this.checkedDecoder.applyBase64Delta(null, null, this.invalidBaseId);
    }

    @Test
    public void applyBase64DeltaShouldSetBaseIdProperlyForChaining() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(this.base, this.baseId);
        this.checkedDecoder.applyBase64Delta(this.base64Delta, this.deltaId, this.baseId);
        DeltaApplicationResult result = this.checkedDecoder.applyBase64Delta(this.base64SecondDelta, this.secondDeltaId, this.deltaId);
        assertNotNull(result);
        assertArrayEquals(this.secondExpectedResult, result.asByteArray());
    }

    private String getSequenceContinuityExceptionMessage(String expectedId, String actualId) {
        return "Sequence continuity check failed - the provided id (" + actualId + ") does not match the last preserved sequence id (" + expectedId + ")";
    }

    @Test
    public void setBaseByteArrayShouldSetBaseIdProperly() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(this.base, this.baseId);
        DeltaApplicationResult result = this.checkedDecoder.applyDelta(this.delta, this.deltaId, this.baseId);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void setBaseStringShouldSetBaseIdProperly() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase(this.stringBase, this.baseId);
        DeltaApplicationResult result = this.checkedDecoder.applyDelta(this.delta, this.deltaId, this.baseId);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }

    @Test
    public void setBase64BaseShouldSetBaseIdProperly() throws IOException, SequenceContinuityException {
        this.checkedDecoder.setBase64Base(this.base64Base, this.baseId);
        DeltaApplicationResult result = this.checkedDecoder.applyDelta(this.delta, this.deltaId, this.baseId);
        assertNotNull(result);
        assertArrayEquals(this.expectedResult, result.asByteArray());
    }
}
