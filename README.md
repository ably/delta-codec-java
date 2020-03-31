# VCDIFF Delta Codec for Java

[![Build Status](https://travis-ci.org/ably/delta-codec-java.svg?branch=master)](https://travis-ci.org/ably/delta-codec-java)

## Overview

This codec wraps [vcdiff-java](https://github.com/ehrmann/vcdiff-java), an implementation of
[RFC 3284](https://tools.ietf.org/html/rfc3284) "The VCDIFF Generic Differencing and Compression Data Format",
making it easier to use VCDIFF for delta applications including with Ably's services.

Supported platforms:

- **Java**: Java 7 or newer
- **Android**: `android-19` or newer as the target SDK, `android-16` or newer as a target platform

## General Use

The `VcdiffDecoder` class is an entry point to the public API. It provides a stateful way of applying a stream of `vcdiff` deltas.

`VcdiffDecoder` can do the necessary bookkeeping in the scenario where a number of successive deltas/patches have to be applied where each of them represents the difference to the previous one (e.g. a sequence of messages each of which represents a set of mutations to a given JavaScript object; i.e. sending only the mutations of an object instead the full object each time).

In order to benefit from the bookkeeping provided by the `VcdiffDecoder` class one has to first provide the base object that the first delta would be generated against. That could be done using the `setBase` method. The most simple flavor of `setBase` is:

```
VcdiffDecoder decoder = new VcdiffDecoder();
decoder.setBase(baseObject /*the base object/message*/);
```

Once the decoder is initialized like this it could be used to apply a stream of deltas/patches each one resulting in a new full payload. E.g. for binary objects/messages:

```
byte[] result = decoder.applyDelta(vcdiffDelta).asByteArray();
```

or for string objects/messages:

```
string result = decoder.applyDelta(vcdiffDelta).asUtf8String();
```

`applyDelta` could be called as many times as needed. The `VcdiffDecoder` will automatically retain the last delta application result and use it as a base for the next delta application. Thus it allows applying an infinite sequence of deltas.

`applyDelta` return type is `DeltaApplicationResult`. That is a convenience class that allows interpreting the result in various data formats - string, array, etc.

`CheckedVcdiffDecoder` is a flavor of `VcdiffDecoder` that could be used if deltas and objects against which deltas are generated have unique IDs. `CheckedVcdiffDecoder`'s `setBase` and `applyDelta` methods require these IDs and make sure the deltas are applied to the objects they were generated against. E.g.

```
DeltaApplicationResult result = checkedDecoder.applyDelta(vcdiffDelta, 
                deltaID,/*any unique identifier of the delta there might be*/ 
                baseID/*any unique identifier of the object this delta was generated against there might be */);
```

There are `base64` flavors of `setBase` and `applyDelta` that would accept `base64` encoded input - `setBase64Base` and `applyBase64Delta`. These are convenience methods and they follow the same logic as `setBase` and `applyDelta`.

## Ably Use

### MQTT with Binary Payload

This is a simple example that uses this codec to handle delta messages received from Ably over an MQTT connection.

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
    }

## Building

A Gradle wrapper is included. The Linux / macOS form of the commands, given below, is:

    ./gradlew <task name>

On Windows there is a batch file:

    gradlew.bat <task name>

This library supports the standard gradle targets; for example, to build the library, use:

    ./gradlew assemble

## Tests

Run tests with:

    ./gradlew test
