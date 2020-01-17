package io.ably.deltasampleapp;

import io.ably.deltacodec.CheckedVcdiffDecoder;
import io.ably.lib.realtime.*;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.Message;
import io.ably.lib.util.Serialisation;

public class Main {
    public static void main(String[] args) throws AblyException {
        AblyRealtime ably = new AblyRealtime("HG2KVw.AjZP_A:W7VXUG9yw1-Cza6u");
        Channel channel = ably.channels.get("[?delta=vcdiff]delta-sample-app");
        CheckedVcdiffDecoder channelDecoder = new CheckedVcdiffDecoder();
        ably.connection.on(ConnectionState.connected, new ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(ConnectionStateChange state) {
                try {
                    channel.subscribe(new ChannelBase.MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            String data = (String)message.data;
                            try {
                                MessageExtras extras = Serialisation.gson.fromJson(message.extras, MessageExtras.class);
                                if (extras != null && extras.delta != null) {
                                    data = channelDecoder.applyDelta(data, message.id, extras.delta.from).asUtf8String();
                                } else {
                                    channelDecoder.setBase(data, message.id);
                                }
                            } catch (Exception e) {
                                /* Delta decoder error */
                            }

                            /* Process decoded data */
                            System.out.println(Serialisation.gson.fromJson((String)data, Data.class).toString());
                        }
                    });
                } catch (AblyException e) {
                    /* Subscribe error */
                }

                Data data = new Data();
                data.foo = "bar";
                data.count = 1;
                data.status = "active";

                try {
                    channel.publish("data", Serialisation.gson.toJson(data));
                    data.count++;
                    channel.publish("data", Serialisation.gson.toJson(data));
                    data.status = "inactive";
                    channel.publish("data", Serialisation.gson.toJson(data));
                } catch (Exception e) {
                    /* Publish error */
                }
            }
        });
    }

    private class MessageExtras {
        public DeltaExtras delta;

        private class DeltaExtras {
            public String format;
            public String from;
        }
    }

    private static class Data {
        public String foo;
        public int count;
        public String status;

        @Override
        public String toString() {
            return "foo = " + this.foo + "; count = " + this.count + "; status = " + this.status;
        }
    }
}
