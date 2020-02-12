package io.ably.mqtt_binary;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import io.ably.deltacodec.VcdiffDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        final String channelName = "sample-app-mqtt";
        final Mqtt3AsyncClient client = createClient();
        final VcdiffDecoder channelDecoder = new VcdiffDecoder();

        connect(client, () -> {
            subscribe(client, "[?delta=vcdiff]" + channelName, (payload) -> {
                byte[] data;
                try {
                    if (VcdiffDecoder.isDelta(payload)) {
                        data = channelDecoder.applyDelta(payload).asByteArray();
                    } else {
                        data = payload;
                        channelDecoder.setBase(data);
                    }
                } catch (Throwable error) {
                    /* Delta decoder error */
                    System.out.println(error.getMessage());
                    return;
                }

                /* Process decoded data */
                System.out.println(Arrays.toString(data));
            });

            publish(client, channelName, new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116 });
            publish(client, channelName, new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46 });
            publish(client, channelName, new byte[] { 76, 111, 114, 101, 109, 32, 105, 112, 115, 117, 109, 32, 100, 111, 108, 111, 114, 32, 115, 105, 116, 32, 97, 109, 101, 116, 44, 32, 99, 111, 110, 115, 101, 99, 116, 101, 116, 117, 114, 32, 97, 100, 105, 112, 105, 115, 99, 105, 110, 103, 32, 101, 108, 105, 116, 46, 32, 70, 117, 115, 99, 101, 32, 105, 100, 32, 110, 117, 108, 108, 97, 32, 108, 97, 99, 105, 110, 105, 97, 44, 32, 118, 111, 108, 117, 116, 112, 97, 116, 32, 111, 100, 105, 111, 32, 117, 116, 44, 32, 117, 108, 116, 114, 105, 99, 101, 115, 32, 108, 105, 103, 117, 108, 97, 46 });
        });
    }

    private static Mqtt3AsyncClient createClient() {
        return Mqtt3Client.builder()
                .serverHost("mqtt.ably.io")
                .serverPort(8883)
                .sslWithDefaultConfig()
                .simpleAuth(
                        Mqtt3SimpleAuth.builder()
                                .username("FIRST_HALF_OF_API_KEY")
                                .password("SECOND_HALF_OF_API_KEY".getBytes(StandardCharsets.UTF_8))
                                .build()
                )
                .buildAsync();
    }

    private static void connect(Mqtt3AsyncClient client, Runnable callback) {
        client.connect().whenComplete((mqtt3ConnAck, throwable) -> {
            if (throwable != null) {
                System.out.println("Connect failed - " + throwable.getMessage());
                return;
            }

            callback.run();
        });
    }

    private static void subscribe(Mqtt3AsyncClient client, String channelName, Consumer<byte[]> callback) {
        client.subscribeWith()
                .topicFilter(channelName)
                .qos(MqttQos.AT_MOST_ONCE)
                .callback(mqtt3Publish -> callback.accept(mqtt3Publish.getPayloadAsBytes()))
                .send();
    }

    private static void publish(Mqtt3AsyncClient client, String channelName, byte[] data) {
        client.publishWith()
                .topic(channelName)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(data)
                .send();
    }
}
