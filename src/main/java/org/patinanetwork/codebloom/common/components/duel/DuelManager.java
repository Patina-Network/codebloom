package org.patinanetwork.codebloom.common.components.duel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyQuestion;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayer;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayerQuestion;
import org.patinanetwork.codebloom.common.db.models.question.bank.QuestionBank;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyQuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.player.LobbyPlayerRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.player.question.LobbyPlayerQuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.question.questionbank.QuestionBankRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.lobby.DuelData;
import org.patinanetwork.codebloom.common.dto.lobby.LobbyDto;
import org.patinanetwork.codebloom.common.dto.question.QuestionBankDto;
import org.patinanetwork.codebloom.common.dto.question.QuestionDto;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.patinanetwork.codebloom.common.leetcode.LeetcodeClient;
import org.patinanetwork.codebloom.common.leetcode.models.LeetcodeSubmission;
import org.patinanetwork.codebloom.common.leetcode.throttled.ThrottledLeetcodeClient;
import org.patinanetwork.codebloom.common.submissions.SubmissionsHandler;
import org.patinanetwork.codebloom.common.submissions.object.AcceptedSubmission;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.common.utils.function.FunctionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DuelManager {

    @FunctionalInterface
    private interface DuelSupplier<T> {
        T get() throws DuelException;
    }

    @FunctionalInterface
    private interface DuelProcedure {
        void run() throws DuelException;
    }

    private static final int MAX_LEETCODE_SUBMISSIONS = 5;

    private final LobbyRepository lobbyRepository;
    private final LobbyQuestionRepository lobbyQuestionRepository;
    private final LobbyPlayerRepository lobbyPlayerRepository;
    private final LobbyPlayerQuestionRepository lobbyPlayerQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final UserRepository userRepository;
    private final LeetcodeClient leetcodeClient;
    private final SubmissionsHandler submissionsHandler;

    public DuelManager(
            final LobbyRepository lobbyRepository,
            final LobbyQuestionRepository lobbyQuestionRepository,
            final LobbyPlayerRepository lobbyPlayerRepository,
            final LobbyPlayerQuestionRepository lobbyPlayerQuestionRepository,
            final QuestionRepository questionRepository,
            final QuestionBankRepository questionBankRepository,
            final UserRepository userRepository,
            final ThrottledLeetcodeClient throttledLeetcodeClient,
            final SubmissionsHandler submissionsHandler) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyQuestionRepository = lobbyQuestionRepository;
        this.lobbyPlayerRepository = lobbyPlayerRepository;
        this.lobbyPlayerQuestionRepository = lobbyPlayerQuestionRepository;
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.userRepository = userRepository;
        this.leetcodeClient = throttledLeetcodeClient;
        this.submissionsHandler = submissionsHandler;
    }

    private Map<String, List<QuestionDto>> buildPlayerSolvedQuestionsMap(final String lobbyId) {
        Map<String, List<QuestionDto>> playerQuestionsMap = new HashMap<>();

        var lobbyPlayers = lobbyPlayerRepository.findPlayersByLobbyId(lobbyId);

        for (var player : lobbyPlayers) {
            var lobbyPlayerQuestions = lobbyPlayerQuestionRepository.findQuestionsByLobbyPlayerId(player.getId());

            List<QuestionDto> playerQuestions = lobbyPlayerQuestions.stream()
                    .filter(lpq -> lpq.getQuestionId().isPresent())
                    .map(lpq -> questionRepository.getQuestionById(
                            lpq.getQuestionId().get()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(QuestionDto::fromQuestion)
                    .collect(Collectors.toList());

            playerQuestionsMap.put(player.getPlayerId(), playerQuestions);
        }

        return playerQuestionsMap;
    }

    private List<UserDto> buildPlayersInLobby(final String lobbyId) {
        var lobbyPlayers = lobbyPlayerRepository.findPlayersByLobbyId(lobbyId);

        return lobbyPlayers.stream()
                .map(lobbyPlayer -> userRepository.getUserById(lobbyPlayer.getPlayerId()))
                .flatMap(Optional::stream)
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
    }

    private <T> T wrap(DuelSupplier<T> supplier) throws DuelException {
        try {
            return supplier.get();
        } catch (DuelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception thrown in DuelManager", e);
            throw new DuelException(e);
        }
    }

    private void wrap(DuelProcedure procedure) throws DuelException {
        try {
            procedure.run();
        } catch (DuelException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception thrown in DuelManager", e);
            throw new DuelException(e);
        }
    }

    public DuelData generateDuelData(final String lobbyId) throws DuelException {
        return wrap(() -> {
            var fetchedLobby = lobbyRepository
                    .findLobbyById(lobbyId)
                    .map(LobbyDto::fromLobby)
                    .orElse(null);

            List<QuestionBankDto> lobbyQuestions = lobbyQuestionRepository.findLobbyQuestionsByLobbyId(lobbyId).stream()
                    .map(lq -> questionBankRepository.getQuestionById(lq.getQuestionBankId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(QuestionBankDto::fromQuestionBank)
                    .collect(Collectors.toList());

            return DuelData.builder()
                    .lobby(fetchedLobby)
                    .questions(lobbyQuestions)
                    .players(buildPlayersInLobby(lobbyId))
                    .playerQuestions(buildPlayerSolvedQuestionsMap(lobbyId))
                    .build();
        });
    }

    /**
     * @param playerId - equivalent to User.id
     * @param isAdminOverride - If user is admin, we can start the duel without needing 2 players.
     */
    public void startDuel(final String playerId, final boolean isAdminOverride) throws DuelException {
        wrap(() -> {
            LobbyPlayer player = lobbyPlayerRepository
                    .findValidLobbyPlayerByPlayerId(playerId)
                    .orElseThrow(() -> new DuelException(HttpStatus.NOT_FOUND, "You are not currently in a party!"));

            Lobby lobby = lobbyRepository
                    .findLobbyById(player.getLobbyId())
                    .orElseThrow(
                            () -> new DuelException(HttpStatus.INTERNAL_SERVER_ERROR, "Hmm, something went wrong."));

            if (lobby.getStatus() != LobbyStatus.AVAILABLE) {
                throw new DuelException(HttpStatus.CONFLICT, "Lobby is not available!");
            }

            if (!isAdminOverride && lobby.getPlayerCount() < 2) {
                throw new DuelException(HttpStatus.CONFLICT, "You must have at least 2 players!");
            }

            lobby.setStatus(LobbyStatus.ACTIVE);
            lobby.setExpiresAt(Optional.of(StandardizedOffsetDateTime.now().plusMinutes(30)));
            lobbyRepository.updateLobby(lobby);

            QuestionBank randomQuestion = questionBankRepository
                    .getRandomQuestion()
                    .orElseThrow(() -> new DuelException(HttpStatus.NOT_FOUND, "No questions available."));

            LobbyQuestion lobbyQuestion = LobbyQuestion.builder()
                    .lobbyId(lobby.getId())
                    .questionBankId(randomQuestion.getId())
                    .userSolvedCount(0)
                    .build();

            lobbyQuestionRepository.createLobbyQuestion(lobbyQuestion);
        });
    }

    public void endDuel(final String lobbyId, boolean isDuelCleanup) throws DuelException {
        wrap(() -> {
            var activeLobby = lobbyRepository
                    .findLobbyById(lobbyId)
                    .orElseThrow(() -> new DuelException(HttpStatus.NOT_FOUND, "Duel cannot be found."));

            if (activeLobby.getStatus() != LobbyStatus.ACTIVE) {
                throw new DuelException(HttpStatus.CONFLICT, "This duel is not currently active.");
            }

            var activeLobbyExpiresAt = activeLobby.getExpiresAt();
            if (!isDuelCleanup
                    && activeLobbyExpiresAt.isPresent()
                    && activeLobbyExpiresAt.get().isAfter(StandardizedOffsetDateTime.now())) {
                throw new DuelException(HttpStatus.CONFLICT, "This duel is not ready for expiration yet.");
            }

            var lobbyPlayers = lobbyPlayerRepository.findPlayersByLobbyId(activeLobby.getId());

            if (lobbyPlayers.isEmpty()) {
                throw new DuelException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No winner can be found because there are no players in the duel. This should not be happening.");
            }

            if (lobbyPlayers.size() == 1) {
                activeLobby.setWinnerId(Optional.of(lobbyPlayers.get(0).getPlayerId()));
            } else {
                var playerOne = lobbyPlayers.get(0);
                var playerTwo = lobbyPlayers.get(1);
                var playerOnePts = playerOne.getPoints();
                var playerTwoPts = playerTwo.getPoints();

                if (playerOnePts == playerTwoPts) {
                    activeLobby.setTie(true);
                } else {
                    var winner = playerOnePts > playerTwoPts ? playerOne : playerTwo;
                    activeLobby.setWinnerId(Optional.of(winner.getPlayerId()));
                }
            }

            activeLobby.setStatus(LobbyStatus.COMPLETED);

            lobbyRepository.updateLobby(activeLobby);
        });
    }

    public Lobby getLobbyByUserId(String userId) throws DuelException {
        return wrap(() -> {
            var lobby = lobbyRepository
                    .findAvailableLobbyByLobbyPlayerPlayerId(userId)
                    .or(() -> lobbyRepository.findActiveLobbyByLobbyPlayerPlayerId(userId))
                    .orElseThrow(() ->
                            new DuelException(HttpStatus.NOT_FOUND, "No duel or party found for the given player."));

            return lobby;
        });
    }

    public Lobby getDuelByUserId(String userId) throws DuelException {
        return wrap(() -> {
            var lobby = lobbyRepository
                    .findActiveLobbyByLobbyPlayerPlayerId(userId)
                    .orElseThrow(() -> new DuelException(HttpStatus.NOT_FOUND, "No duel found for the given player."));

            return lobby;
        });
    }

    public int processSubmissions(User user, Lobby activeLobby) throws DuelException {
        return wrap(() -> {
            var lobbyPlayer = lobbyPlayerRepository
                    .findValidLobbyPlayerByPlayerId(user.getId())
                    .orElseThrow(() -> new DuelException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "A duel was found but the player instance cannot be found."));

            List<LobbyQuestion> lobbyQuestions =
                    lobbyQuestionRepository.findLobbyQuestionsByLobbyId(activeLobby.getId());

            var solvableQuestionTitlesSet = lobbyQuestions.stream()
                    .map(LobbyQuestion::getQuestionBankId)
                    .map(questionBankRepository::getQuestionById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(QuestionBank::getQuestionTitle)
                    .collect(Collectors.toSet());

            List<LeetcodeSubmission> leetcodeSubmissions = leetcodeClient.findSubmissionsByUsername(
                    user.getLeetcodeUsername().orElse(null), MAX_LEETCODE_SUBMISSIONS);

            var solvedLeetcodeSubmissions = leetcodeSubmissions.stream()
                    .filter(s -> solvableQuestionTitlesSet.contains(s.getTitle()))
                    .toList();

            List<AcceptedSubmission> acceptedSubmissions =
                    submissionsHandler.handleSubmissions(solvedLeetcodeSubmissions, user, true);

            List<LobbyPlayerQuestion> lobbyPlayerQuestions = acceptedSubmissions.stream()
                    .map(s -> LobbyPlayerQuestion.builder()
                            .lobbyPlayerId(lobbyPlayer.getId())
                            // should not be null, but let's not end up in a crashed state because of this.
                            .questionId(Optional.ofNullable(s.questionId()))
                            .points(Optional.of(s.points()))
                            .build())
                    .toList();

            lobbyPlayerQuestions.forEach(
                    q -> FunctionUtils.swallow(() -> lobbyPlayerQuestionRepository.createLobbyPlayerQuestion(q)));

            lobbyPlayer.setPoints(acceptedSubmissions.stream()
                    .mapToInt(AcceptedSubmission::points)
                    .sum());
            lobbyPlayerRepository.updateLobbyPlayer(lobbyPlayer);

            return lobbyPlayerQuestions.size();
        });
    }
}
