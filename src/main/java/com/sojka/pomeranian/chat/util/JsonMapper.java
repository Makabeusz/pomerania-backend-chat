package com.sojka.pomeranian.chat.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;

public class JsonMapper {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    private static final ObjectReader reader = mapper.reader();

    @Getter
    private static final ObjectWriter writer = mapper.writer();

}
