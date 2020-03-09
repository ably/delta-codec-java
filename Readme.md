# Java Codec Library for the VCDIFF Delta Format

## Overview

This repository contains an java implementation of the [VCDiff Application Library](https://github.com/ably/wiki/issues/380#issuecomment-533647591) that provides functionality for applying `vcdiff` deltas/patches. `vcdiff` is a [format for representing deltas](https://tools.ietf.org/html/rfc3284) - differences between data sets (be it files, messages, etc.) 


## Supported Platforms

This SDK supports the following platforms:

**Java:** Java 7+

**Android:** android-19 or newer as a target SDK, android-16 or newer as a target platform

We regression-test the library against a selection of Java and Android platforms (which will change over time, but usually consists of the versions that are supported upstream). Please refer to [.travis.yml](./.travis.yml) for the set of versions that currently undergo CI testing.

We'll happily support (and investigate reported problems with) any reasonably-widely-used platform, Java or Android.
If you find any compatibility issues, please [do raise an issue](https://github.com/ably/delta-codec-java/issues/new) in this repository or [contact Ably customer support](https://support.ably.io/) for advice.

## Installation ##

Reference the library by including a compile dependency reference in your gradle build file.

This [library](https://bintray.com/ably-io/ably/delta-codec-java/_latestVersion) supports both Java and Android.

```
compile 'io.ably:ably-delta-codec-java:1.0.0'
```

The library is hosted on the [Jcenter repository](https://bintray.com/ably-io/ably), so you need to ensure that the repo is referenced also; IDEs will typically include this by default:

```
repositories {
	jcenter()
}
```


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

## Common Use Cases

### Ably Related

#### MQTT with Binary Payload

This is a simple example of how one can utilize the `delta-codec-java` to handle delta messages received by Ably via MQTT

```
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

```

### Non Ably Related

#### Object Mutations Store/Retrieve

VCDiff encoded deltas could be used to efficiently store the history of the mutations of a given instance of a class. Instead of preserving full copies of the instance state at various points of time in its existence one could preserve just the differences between two successive copies, i.e. the delta. The following method can then be used to restore the full copies of the object with the help of `delta-codec-java` lib:

```
    Foo[] getObjectMutationsHistory(Foo initialState, 
                                byte[][] objectMutationsDeltas /*array of vcdiff deltas computed between each two successive states of the base object with deltas being computed on JSON serialized object */) {

        final VcdiffDecoder stateDecoder = new DeltaCodec.VcdiffDecoder();

        stateDecoder.setBase(initialState);
        List<Foo> objectMutations = new ArrayList<Foo>(objectMutationsDeltas.length]);
        
        for(byte[] state : objectMutationsDeltas) {
            try {
                String jsonSerializedObject = stateDecoder.applyDelta(state).asUtf8String();
                objectMutations.add(JSON.parse(serialzedObject));
            }
            catch (e) {
                console.log(e);
                /* Delta decoder error */
            }
        }
        
        objectMutationsDeltas.forEach(state => {
            try {
                let serialzedObject = stateDecoder.applyDelta(state).asUtf8String();
                objectMutations.push(JSON.parse(serialzedObject));
            }  catch (Throwable error) {
                    /* Delta decoder error */
                    System.out.println(error.getMessage());
                    return;
            }
        });
        
        return objectMutations.toArray();
    }
```

## Building ##

A gradle wrapper is included. The Linux/OSX form of the commands, given below, is:

    ./gradlew <task name>

but on Windows there is a batch file:

    gradlew.bat <task name>

This library supports the standard gradle targets; for example, to build the library, use:

    ./gradlew assemble

## Tests

Run tests with:

    ./gradlew test

To run tests against a specific host, specify in the environment:

    env ABLY_ENV=staging ./gradlew test

Tests will run against sandbox by default.

## Release process

This library uses [semantic versioning](http://semver.org/). For each release, the following needs to be done:

### Release notes

* Create a branch for the release, named like `release-1.0.0`
* Replace all references of the current version number with the new version number (check this file [README.md](./README.md) and [build.gradle](./common.gradle)) and commit the changes
* Run [`github_changelog_generator`](https://github.com/skywinder/Github-Changelog-Generator) to update the [CHANGELOG](./CHANGELOG.md): `github_changelog_generator -u ably -p ably-java --header-label="# Changelog" --release-branch=release-1.1.3 --future-release=v1.0.8` 
* Commit [CHANGELOG](./CHANGELOG.md)
* Add a tag and push to origin such as `git tag v1.1.0; git push origin v1.1.0`
* Make a PR against `develop`
* Once the PR is approved, merge it into `develop`
* Fast-forward the master branch: `git checkout master && git merge --ff-only develop && git push origin master`

### Build release

* Run `gradle assemble` to build the JAR for the release

### Publishing to JCenter (Maven)

* Go to the home page for the package; eg https://bintray.com/ably-io/ably/ably-delta-codec-java. Select [New version](https://bintray.com/ably-io/ably/ably-delta-codec-java/new/version), enter the new version such as "1.1.3" in name and save
* Run `./gradlew java:assembleRelease` locally to generate the files
* Open local relative folder such as `./build/release/1.0.0/io/ably/ably-java/1.0.0`
* Then go to the new version in JFrog Bintray; eg https://bintray.com/ably-io/ably/ably-delta-codec-java/1.0.0, then click on the link to upload via the UI in the "Upload files" section
* Type in `io/ably/ably-delta-codec-java/1.0.0` into "Target Repository Path" ensuring the correct version is included. The drag in the files in `build/release/1.0.0/io/ably/ably-java/1.0.0/`. Upload all the `.jar` files and the `.pom` file.
* You will see a notice "You have 1 unpublished item(s) for this version", make sure you click "Publish". Wait a few minutes and check that your version has all the necessary files at https://bintray.com/ably-io/ably/ably-delta-codec-java/1.0.0?sort=&order=#files/io/ably/ably-delta-codec-java/1.0.0 for example.
* Update the README text in Bintray.

### Create release on Github

* Visit [https://github.com/ably/ably-delta-codec-java/tags](https://github.com/ably/ably-delta-codec-java/tags) and `Add release notes` for the release including links to the changelog entry and the JCenter releases.

## Support, feedback and troubleshooting

Please visit http://support.ably.io/ for access to our knowledgebase and to ask for any assistance.

You can also view the [community reported Github issues](https://github.com/ably/ably-delta-codec-java/issues).

To see what has changed in recent versions of this repo, see the [CHANGELOG](CHANGELOG.md).

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Ensure you have added suitable tests and the test suite is passing(`./gradlew java:test android:connectedAndroidTest`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

## License

Copyright (c) 2019 Ably Real-time Ltd, Licensed under the Apache License, Version 2.0.  Refer to [LICENSE](LICENSE) for the license terms.
