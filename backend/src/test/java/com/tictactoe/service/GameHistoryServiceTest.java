package com.tictactoe.service;

import com.tictactoe.dto.GameHistoryResponse;
import com.tictactoe.entity.GameParticipant;
import com.tictactoe.entity.GameRecord;
import com.tictactoe.entity.GameResult;
import com.tictactoe.entity.ParticipantOutcome;
import com.tictactoe.entity.ParticipantType;
import com.tictactoe.entity.PlayerProfile;
import com.tictactoe.exception.ResourceNotFoundException;
import com.tictactoe.model.Difficulty;
import com.tictactoe.model.GameMode;
import com.tictactoe.model.GameStatus;
import com.tictactoe.model.PlayerSymbol;
import com.tictactoe.repository.GameRecordRepository;
import com.tictactoe.repository.PlayerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameHistoryServiceTest {

    @Mock
    private GameRecordRepository gameRecordRepository;

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    private GameHistoryService gameHistoryService;

    @BeforeEach
    void setUp() {
        gameHistoryService = new GameHistoryService(gameRecordRepository, playerProfileRepository);
    }

    @Test
    @DisplayName("Should return null and not persist when userId is null (Guest play)")
    void shouldNotPersistGuestGame() {
        GameRecord result = gameHistoryService.recordCompletedComputerGame(
            null, Difficulty.HARD, GameStatus.PLAYER_WON, Instant.now(), Instant.now()
        );

        assertThat(result).isNull();
        verify(gameRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should record player win against computer authoritatively")
    void shouldRecordPlayerWin() {
        PlayerProfile player = new PlayerProfile("hero");
        player.setId(100L);
        when(playerProfileRepository.findById(100L)).thenReturn(Optional.of(player));

        Instant start = Instant.now().minusSeconds(30);
        Instant end = Instant.now();

        when(gameRecordRepository.save(any(GameRecord.class))).thenAnswer(i -> i.getArgument(0));

        GameRecord saved = gameHistoryService.recordCompletedComputerGame(
            100L, Difficulty.HARD, GameStatus.PLAYER_WON, start, end
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getGameMode()).isEqualTo(GameMode.COMPUTER);
        assertThat(saved.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.getStatus()).isEqualTo(GameStatus.PLAYER_WON);
        assertThat(saved.getResult()).isEqualTo(GameResult.PLAYER_WIN);

        assertThat(saved.getParticipants()).hasSize(2);

        GameParticipant human = saved.getParticipants().stream()
            .filter(p -> p.getParticipantType() == ParticipantType.HUMAN)
            .findFirst().orElseThrow();
        assertThat(human.getPlayer().getId()).isEqualTo(100L);
        assertThat(human.getSymbol()).isEqualTo(PlayerSymbol.X);
        assertThat(human.getOutcome()).isEqualTo(ParticipantOutcome.WIN);

        GameParticipant computer = saved.getParticipants().stream()
            .filter(p -> p.getParticipantType() == ParticipantType.COMPUTER)
            .findFirst().orElseThrow();
        assertThat(computer.getPlayer()).isNull();
        assertThat(computer.getSymbol()).isEqualTo(PlayerSymbol.O);
        assertThat(computer.getOutcome()).isEqualTo(ParticipantOutcome.LOSS);
    }

    @Test
    @DisplayName("Should record computer win against player authoritatively")
    void shouldRecordComputerWin() {
        PlayerProfile player = new PlayerProfile("challenger");
        player.setId(200L);
        when(playerProfileRepository.findById(200L)).thenReturn(Optional.of(player));

        when(gameRecordRepository.save(any(GameRecord.class))).thenAnswer(i -> i.getArgument(0));

        GameRecord saved = gameHistoryService.recordCompletedComputerGame(
            200L, Difficulty.MEDIUM, GameStatus.COMPUTER_WON, Instant.now(), Instant.now()
        );

        assertThat(saved.getResult()).isEqualTo(GameResult.COMPUTER_WIN);

        GameParticipant human = saved.getParticipants().stream()
            .filter(p -> p.getParticipantType() == ParticipantType.HUMAN)
            .findFirst().orElseThrow();
        assertThat(human.getOutcome()).isEqualTo(ParticipantOutcome.LOSS);

        GameParticipant computer = saved.getParticipants().stream()
            .filter(p -> p.getParticipantType() == ParticipantType.COMPUTER)
            .findFirst().orElseThrow();
        assertThat(computer.getOutcome()).isEqualTo(ParticipantOutcome.WIN);
    }

    @Test
    @DisplayName("Should record draw outcome correctly")
    void shouldRecordDraw() {
        PlayerProfile player = new PlayerProfile("player");
        player.setId(300L);
        when(playerProfileRepository.findById(300L)).thenReturn(Optional.of(player));
        when(gameRecordRepository.save(any(GameRecord.class))).thenAnswer(i -> i.getArgument(0));

        GameRecord saved = gameHistoryService.recordCompletedComputerGame(
            300L, Difficulty.EASY, GameStatus.DRAW, Instant.now(), Instant.now()
        );

        assertThat(saved.getResult()).isEqualTo(GameResult.DRAW);
        assertThat(saved.getParticipants()).allMatch(p -> p.getOutcome() == ParticipantOutcome.DRAW);
    }

    @Test
    @DisplayName("Should reject non-terminal game statuses")
    void shouldRejectNonTerminalStatus() {
        PlayerProfile player = new PlayerProfile("player");
        player.setId(1L);
        when(playerProfileRepository.findById(1L)).thenReturn(Optional.of(player));

        assertThrows(IllegalArgumentException.class, () -> gameHistoryService.recordCompletedComputerGame(
            1L, Difficulty.EASY, GameStatus.IN_PROGRESS, Instant.now(), Instant.now()
        ));
    }

    @Test
    @DisplayName("Should retrieve paginated user history with proper mapping")
    void shouldRetrieveUserHistory() {
        when(playerProfileRepository.existsById(1L)).thenReturn(true);

        PlayerProfile player = new PlayerProfile("gamer");
        player.setId(1L);

        GameRecord record = new GameRecord(GameMode.COMPUTER, Difficulty.HARD, GameStatus.PLAYER_WON, GameResult.PLAYER_WIN,
            Instant.now().minusSeconds(60), Instant.now());
        record.setId(501L);
        record.addParticipant(new GameParticipant(record, player, PlayerSymbol.X, ParticipantType.HUMAN, ParticipantOutcome.WIN));
        record.addParticipant(new GameParticipant(record, null, PlayerSymbol.O, ParticipantType.COMPUTER, ParticipantOutcome.LOSS));

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(gameRecordRepository.findByPlayerId(1L, pageRequest))
            .thenReturn(new PageImpl<>(List.of(record), pageRequest, 1));

        Page<GameHistoryResponse> historyPage = gameHistoryService.getUserHistory(1L, pageRequest);

        assertThat(historyPage.getTotalElements()).isEqualTo(1);
        GameHistoryResponse entry = historyPage.getContent().get(0);
        assertThat(entry.getGameId()).isEqualTo(501L);
        assertThat(entry.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(entry.getOutcome()).isEqualTo(ParticipantOutcome.WIN);
        assertThat(entry.getOpponent()).isEqualTo("Computer");
        assertThat(entry.getDurationSeconds()).isGreaterThanOrEqualTo(59);
    }
}
