package ru.romanow.core.commons.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by ronin on 28.09.16
 */
public class JsonSerializer {

    private static Gson gson = new GsonBuilder().create();
    private static Gson prettyJson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static String toPrettyJson(Object object) {
        return prettyJson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> cls) {
        return gson.fromJson(json, cls);
    }
}
