package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResultsPage<T> {

    private List<T> results;
    private String nextPageState;
}
