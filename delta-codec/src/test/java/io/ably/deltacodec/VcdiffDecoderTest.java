package io.ably.deltacodec;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;

public class VcdiffDecoderTest extends BaseVcdiffDecoderTests {
    private VcdiffDecoder decoder;

    @Before
    public void createVcdiffDecoderInstance() {
        this.decoder = new VcdiffDecoder();
    }

    @After
    public void disposeVcdiffDecoderInstance() {
        this.decoder = null;
    }

    @Override
    protected DeltaApplicationResult applyDelta(byte[] delta) throws IOException {
        return this.decoder.applyDelta(delta);
    }

    @Override
    protected DeltaApplicationResult applyBase64Delta(String delta) throws IOException {
        return this.decoder.applyBase64Delta(delta);
    }

    @Override
    protected void setBase(byte[] newBase) {
        this.decoder.setBase(newBase);
    }

    @Override
    protected void setBase(String newBase) {
        this.decoder.setBase(newBase);
    }

    @Override
    protected void setBase64Base(String newBase) {
        this.decoder.setBase64Base(newBase);
    }
}
