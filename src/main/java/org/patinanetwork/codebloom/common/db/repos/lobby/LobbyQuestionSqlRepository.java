package org.patinanetwork.codebloom.common.db.repos.lobby;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyQuestion;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LobbyQuestionSqlRepository implements LobbyQuestionRepository {

    private static final RowMapper<LobbyQuestion> LOBBY_QUESTION_ROW_MAPPER = (rs, rowNum) -> LobbyQuestion.builder()
            .id(rs.getString("id"))
            .lobbyId(rs.getString("lobbyId"))
            .questionBankId(rs.getString("questionBankId"))
            .userSolvedCount(rs.getInt("userSolvedCount"))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .build();

    private final JdbcClient jdbcClient;

    public LobbyQuestionSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createLobbyQuestion(final LobbyQuestion lobbyQuestion) {
        String sql = """
            INSERT INTO "LobbyQuestion"
                (id, "lobbyId", "questionBankId", "userSolvedCount")
            VALUES
                (:id, :lobbyId, :questionBankId, :userSolvedCount)
            RETURNING
                "createdAt"
            """;
        lobbyQuestion.setId(UUID.randomUUID().toString());

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(lobbyQuestion.getId()))
                .param("lobbyId", UUID.fromString(lobbyQuestion.getLobbyId()))
                .param("questionBankId", UUID.fromString(lobbyQuestion.getQuestionBankId()))
                .param("userSolvedCount", lobbyQuestion.getUserSolvedCount())
                .query((rs, rowNum) -> rs.getObject("createdAt", OffsetDateTime.class))
                .optional()
                .orElse(null);

        lobbyQuestion.setCreatedAt(createdAt);
    }

    @Override
    public Optional<LobbyQuestion> findLobbyQuestionById(final String id) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "questionBankId",
                "userSolvedCount",
                "createdAt"
            FROM
                "LobbyQuestion"
            WHERE
                id = :id
            """;
        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(LOBBY_QUESTION_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<LobbyQuestion> findLobbyQuestionsByLobbyId(final String lobbyId) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "questionBankId",
                "userSolvedCount",
                "createdAt"
            FROM
                "LobbyQuestion"
            WHERE
                "lobbyId" = :lobbyId
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyId", UUID.fromString(lobbyId))
                .query(LOBBY_QUESTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<LobbyQuestion> findLobbyQuestionsByLobbyIdAndQuestionBankId(
            final String lobbyId, final String questionBankId) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "questionBankId",
                "userSolvedCount",
                "createdAt"
            FROM
                "LobbyQuestion"
            WHERE
                "lobbyId" = :lobbyId AND "questionBankId" = :questionBankId
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyId", UUID.fromString(lobbyId))
                .param("questionBankId", UUID.fromString(questionBankId))
                .query(LOBBY_QUESTION_ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<LobbyQuestion> findMostRecentLobbyQuestionByLobbyId(final String lobbyId) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "questionBankId",
                "userSolvedCount",
                "createdAt"
            FROM
                "LobbyQuestion"
            WHERE
                "lobbyId" = :lobbyId
            ORDER BY
                "createdAt" DESC
            LIMIT 1
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyId", UUID.fromString(lobbyId))
                .query(LOBBY_QUESTION_ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean updateQuestionLobby(final LobbyQuestion lobbyQuestion) {
        String sql = """
            UPDATE "LobbyQuestion"
            SET
                "userSolvedCount" = :userSolvedCount
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("userSolvedCount", lobbyQuestion.getUserSolvedCount())
                .param("id", UUID.fromString(lobbyQuestion.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public List<LobbyQuestion> findAllLobbyQuestions() {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "questionBankId",
                "userSolvedCount",
                "createdAt"
            FROM
                "LobbyQuestion"
            """;

        return jdbcClient.sql(sql).query(LOBBY_QUESTION_ROW_MAPPER).list();
    }

    @Override
    public boolean deleteLobbyQuestionById(final String id) {
        String sql = """
            DELETE FROM "LobbyQuestion"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected == 1;
    }
}
