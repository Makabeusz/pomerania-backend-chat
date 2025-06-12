package com.sojka.pomeranian.chat.repository;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.sojka.pomeranian.chat.dto.ResultsPage;
import com.sojka.pomeranian.chat.exception.AstraException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Abstract base class for repositories interacting with DataStax Astra, a cloud-native Cassandra database.
 * Provides utility methods for common operations, such as decoding and encoding pagination states
 * and constructing paginated result pages.
 * <p>
 * This class simplifies integration with Astra by handling pagination state management and result
 * page creation, ensuring consistent and efficient data retrieval across repository implementations.
 * It is designed to be extended by concrete repository classes that perform CRUD operations
 * using the DataStax Cassandra driver.
 * </p>
 *
 * @see com.datastax.oss.driver.api.core.cql.ResultSet
 * @see ResultsPage
 * @see AstraException
 */
public abstract class AstraRepository {

    /**
     * Decodes a Base64-encoded pagination state string into a {@link ByteBuffer} for use in
     * Cassandra queries.
     *
     * @param pageState the Base64 URL-encoded pagination state string, or {@code null} if no
     *                  pagination state is provided (no more page exists)
     * @return A {@link ByteBuffer} containing the decoded pagination state, or {@code null} if
     * the input is {@code null} (no more pages exists).
     * @throws AstraException if the {@code pageState} is invalid or cannot be decoded due to
     *                        improper Base64 formatting
     */
    public ByteBuffer decodePageState(String pageState) {
        if (pageState != null) {
            try {
                return ByteBuffer.wrap(Base64.getUrlDecoder().decode(pageState));
            } catch (IllegalArgumentException e) {
                throw new AstraException("Invalid pageState", e);
            }
        }
        return null;
    }

    /**
     * Constructs a {@link ResultsPage} containing the current page of results and the next
     * pagination state for subsequent queries. This method facilitates paginated data retrieval
     * by combining query results with a Base64-encoded pagination state derived from the
     * Cassandra {@link ResultSet}.
     *
     * @param <T>       the type of entities in the result list
     * @param results   the list of entities retrieved in the current query
     * @param resultSet the Cassandra {@link ResultSet} containing query execution metadata,
     *                  including the pagination state
     * @return a {@link ResultsPage} containing the provided results and the Base64-encoded
     * pagination state for the next page, or {@code null} if no further pages exist
     */
    public <T> ResultsPage<T> resultsPage(ResultSet resultSet, int pageSize, Function<Row, T> mapper) {
        List<T> results = new ArrayList<>();
        int rowCount = 0;

        for (Row row : resultSet) {
            results.add(mapper.apply(row));

            if (++rowCount >= pageSize) {
                break;
            }
        }

        var pagingState = resultSet.getExecutionInfo().getPagingState();
        var nextPageState = pagingState != null ? Base64.getUrlEncoder().withoutPadding().encodeToString(pagingState.array()) : null;

        return new ResultsPage<>(results, nextPageState);

    }

}
