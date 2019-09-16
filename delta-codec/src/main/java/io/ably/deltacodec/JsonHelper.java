package io.ably.deltacodec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

class JsonHelper {
    private static final JsonHelper instance = new JsonHelper();

    private final Gson gson = new Gson();

    static JsonHelper getInstance() {
        return instance;
    }

    String serialize(Object obj) {
        return this.gson.toJson(obj);
    }

    Object deserialize(String str) {
        return this.gson.fromJson(str, JsonElement.class);
    }
}
