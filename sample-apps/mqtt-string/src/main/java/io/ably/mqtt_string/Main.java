package io.ably.mqtt_string;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import io.ably.deltacodec.VcdiffDecoder;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) {
        final String channelName = "sample-app-mqtt";
        final Mqtt3AsyncClient client = createClient();
        final VcdiffDecoder channelDecoder = new VcdiffDecoder();

        connect(client, () -> {
            subscribe(client, "[?delta=vcdiff]" + channelName, (payload) -> {
                String data;
                try {
                    if (VcdiffDecoder.isDelta(payload)) {
                        data = channelDecoder.applyDelta(payload).asUtf8String();
                    } else {
                        data = new String(payload);
                        channelDecoder.setBase(data);
                    }
                } catch (Throwable error) {
                    /* Delta decoder error */
                    System.out.println(error.getMessage());
                    return;
                }

                /* Process decoded data */
                System.out.println(data);
            });

            publish(client, channelName, "Lorem ipsum dolor sit amet");
            publish(client, channelName, "Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
            publish(client, channelName, "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus a elit fermentum felis feugiat lacinia.");
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

    private static void publish(Mqtt3AsyncClient client, String channelName, String data) {
        client.publishWith()
                .topic(channelName)
                .qos(MqttQos.AT_MOST_ONCE)
                .payload(data.getBytes())
                .send();
    }
}
