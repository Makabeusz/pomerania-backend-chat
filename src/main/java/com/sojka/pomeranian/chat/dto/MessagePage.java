package com.sojka.pomeranian.chat.dto;

import com.sojka.pomeranian.chat.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MessagePage {

    private List<Message> messages;
    private String nextPageState;
}
