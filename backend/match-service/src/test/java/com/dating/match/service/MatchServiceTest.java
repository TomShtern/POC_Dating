package com.dating.match.service;

import com.dating.common.exception.MatchNotFoundException;
import com.dating.match.client.UserServiceClient;
import com.dating.match.dto.response.MatchDetailResponse;
import com.dating.match.dto.response.MatchListResponse;
import com.dating.match.event.MatchEventPublisher;
import com.dating.match.exception.UnauthorizedMatchAccessException;
import com.dating.match.model.Match;
import com.dating.match.model.MatchScore;
import com.dating.match.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchService.
 */
@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MatchEventPublisher eventPublisher;

    @InjectMocks
    private MatchService matchService;

    private UUID userId;
    private UUID otherUserId;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        matchId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should return paginated list of matches")
    void getMatches_Success() {
        // Arrange
        Match match = createMatch();
        Page<Match> matchPage = new PageImpl<>(List.of(match), PageRequest.of(0, 20), 1);

        when(matchRepository.findActiveMatchesByUserId(eq(userId), any(PageRequest.class)))
                .thenReturn(matchPage);

        when(userServiceClient.getUserById(otherUserId)).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        otherUserId, "other@test.com", "otheruser", "Other", "User",
                        25, "FEMALE", "Bio", "http://photo.jpg", "ACTIVE"));

        // Act
        MatchListResponse response = matchService.getMatches(userId, 20, 0);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.matches().size());
        assertEquals(1, response.total());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("Should return empty list when no matches")
    void getMatches_Empty() {
        // Arrange
        Page<Match> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(matchRepository.findActiveMatchesByUserId(eq(userId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        // Act
        MatchListResponse response = matchService.getMatches(userId, 20, 0);

        // Assert
        assertNotNull(response);
        assertTrue(response.matches().isEmpty());
        assertEquals(0, response.total());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("Should return match details")
    void getMatchDetails_Success() {
        // Arrange
        Match match = createMatchWithScore();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        when(userServiceClient.getUserById(any())).thenReturn(
                new UserServiceClient.UserProfileResponse(
                        userId, "test@test.com", "testuser", "Test", "User",
                        25, "MALE", "Bio", "http://photo.jpg", "ACTIVE"));

        // Act
        MatchDetailResponse response = matchService.getMatchDetails(matchId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(matchId, response.id());
        assertNotNull(response.matchScore());
        assertNotNull(response.scoreFactors());
    }

    @Test
    @DisplayName("Should throw MatchNotFoundException when match not found")
    void getMatchDetails_NotFound() {
        // Arrange
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MatchNotFoundException.class, () ->
                matchService.getMatchDetails(matchId, userId));
    }

    @Test
    @DisplayName("Should throw UnauthorizedMatchAccessException when user not in match")
    void getMatchDetails_Unauthorized() {
        // Arrange
        UUID otherUser1 = UUID.randomUUID();
        UUID otherUser2 = UUID.randomUUID();

        Match match = Match.builder()
                .id(matchId)
                .user1Id(otherUser1)
                .user2Id(otherUser2)
                .matchedAt(Instant.now())
                .build();

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(UnauthorizedMatchAccessException.class, () ->
                matchService.getMatchDetails(matchId, userId));
    }

    @Test
    @DisplayName("Should unmatch successfully")
    void unmatch_Success() {
        // Arrange
        Match match = createMatch();

        when(matchRepository.findByIdAndUserId(matchId, userId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenReturn(match);

        // Act
        matchService.unmatch(matchId, userId);

        // Assert
        verify(matchRepository).save(any(Match.class));
        verify(eventPublisher).publishMatchEnded(any(Match.class), eq(userId));
        assertNotNull(match.getEndedAt());
    }

    @Test
    @DisplayName("Should throw MatchNotFoundException when unmatching non-existent match")
    void unmatch_NotFound() {
        // Arrange
        when(matchRepository.findByIdAndUserId(matchId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MatchNotFoundException.class, () ->
                matchService.unmatch(matchId, userId));

        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when unmatching already ended match")
    void unmatch_AlreadyEnded() {
        // Arrange
        Match match = createMatch();
        match.setEndedAt(Instant.now());

        when(matchRepository.findByIdAndUserId(matchId, userId)).thenReturn(Optional.of(match));

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
                matchService.unmatch(matchId, userId));

        verify(matchRepository, never()).save(any(Match.class));
    }

    private Match createMatch() {
        return Match.builder()
                .id(matchId)
                .user1Id(userId.compareTo(otherUserId) < 0 ? userId : otherUserId)
                .user2Id(userId.compareTo(otherUserId) < 0 ? otherUserId : userId)
                .matchedAt(Instant.now())
                .build();
    }

    private Match createMatchWithScore() {
        Match match = createMatch();

        MatchScore score = MatchScore.builder()
                .id(UUID.randomUUID())
                .match(match)
                .score(BigDecimal.valueOf(85))
                .factors(Map.of("interestMatch", 40, "ageCompatibility", 30))
                .calculatedAt(Instant.now())
                .build();

        match.setMatchScore(score);
        return match;
    }
}
