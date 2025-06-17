package com.sojka.pomeranian.chat.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;

import java.io.IOException;

/**
 * TODO: DUPLICATED in main (JsonUtils.java), chat(this)
 */
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    private static final ObjectReader reader = mapper.reader();

    @Getter
    private static final ObjectWriter writer = mapper.writer();

    public static String writeToString(Object object) {
        try {
            return writer.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readObject(byte[] bytes, Class<T> aClass) {
        try {
            return reader.readValue(bytes, aClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
