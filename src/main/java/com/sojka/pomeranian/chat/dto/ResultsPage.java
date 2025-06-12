package com.sojka.pomeranian.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * The paged results type. Can keep either DTO or model objects.
 *
 * @param <T> Results type
 */
@Data
@AllArgsConstructor
public class ResultsPage<T> {

    private List<T> results;
    private String nextPageState;
}
