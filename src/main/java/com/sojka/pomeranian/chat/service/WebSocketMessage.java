package com.sojka.pomeranian.chat.service;

public record WebSocketMessage<T>(String destination, T payload) {
}
