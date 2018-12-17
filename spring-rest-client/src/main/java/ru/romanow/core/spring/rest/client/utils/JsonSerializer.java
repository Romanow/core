package ru.romanow.core.spring.rest.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class JsonSerializer {

    private static Gson gson = new GsonBuilder().create();
    private static Gson prettyJson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static String toJson(@Nullable Object object) {
        return gson.toJson(object);
    }

    public static String toPrettyJson(@Nullable Object object) {
        return prettyJson.toJson(object);
    }

    public static <T> T fromJson(@Nullable String json, @Nonnull Class<T> cls) {
        return gson.fromJson(json, cls);
    }
}
