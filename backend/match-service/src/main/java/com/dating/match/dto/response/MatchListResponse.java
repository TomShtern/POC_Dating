package com.dating.match.dto.response;

import java.util.List;

/**
 * Response DTO for paginated list of matches.
 *
 * @param matches List of matches
 * @param total Total number of matches
 * @param limit Page size
 * @param offset Current offset
 * @param hasMore Whether more matches are available
 */
public record MatchListResponse(
    List<MatchResponse> matches,
    long total,
    int limit,
    int offset,
    boolean hasMore
) {}
