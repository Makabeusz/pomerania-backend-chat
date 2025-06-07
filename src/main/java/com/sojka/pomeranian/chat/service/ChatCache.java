package com.sojka.pomeranian.chat.service;

public interface ChatCache {

    boolean isOnline(String userId);

    boolean put(String userId);

    boolean remove(String userId);
}
