package org.patinanetwork.codebloom.common.db.repos.lobby.player.question;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayerQuestion;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LobbyPlayerQuestionSqlRepository implements LobbyPlayerQuestionRepository {

    private static final RowMapper<LobbyPlayerQuestion> LOBBY_PLAYER_QUESTION_ROW_MAPPER =
            (rs, rowNum) -> LobbyPlayerQuestion.builder()
                    .id(rs.getString("id"))
                    .lobbyPlayerId(rs.getString("lobbyPlayerId"))
                    .questionId(Optional.ofNullable(rs.getString("questionId")))
                    .points(Optional.ofNullable(rs.getObject("points", Integer.class)))
                    .build();

    private final JdbcClient jdbcClient;

    public LobbyPlayerQuestionSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createLobbyPlayerQuestion(final LobbyPlayerQuestion lobbyPlayerQuestion) {
        String sql = """
            INSERT INTO "LobbyPlayerQuestion"
                (id, "lobbyPlayerId", "questionId", points)
            VALUES
                (:id, :lobbyPlayerId, :questionId, :points)
            """;

        lobbyPlayerQuestion.setId(UUID.randomUUID().toString());

        jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(lobbyPlayerQuestion.getId()))
                .param("lobbyPlayerId", UUID.fromString(lobbyPlayerQuestion.getLobbyPlayerId()))
                .param(
                        "questionId",
                        lobbyPlayerQuestion
                                .getQuestionId()
                                .map(UUID::fromString)
                                .orElse(null))
                .param("points", lobbyPlayerQuestion.getPoints().orElse(null), Types.INTEGER)
                .update();
    }

    @Override
    public Optional<LobbyPlayerQuestion> findLobbyPlayerQuestionById(final String id) {
        String sql = """
            SELECT
                id,
                "lobbyPlayerId",
                "questionId",
                points
            FROM
                "LobbyPlayerQuestion"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(LOBBY_PLAYER_QUESTION_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<LobbyPlayerQuestion> findQuestionsByLobbyPlayerId(final String lobbyPlayerId) {
        String sql = """
            SELECT
                id,
                "lobbyPlayerId",
                "questionId",
                points
            FROM
                "LobbyPlayerQuestion"
            WHERE
                "lobbyPlayerId" = :lobbyPlayerId
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyPlayerId", UUID.fromString(lobbyPlayerId))
                .query(LOBBY_PLAYER_QUESTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<LobbyPlayerQuestion> findLobbyPlayerQuestionsByQuestionId(final String questionId) {
        String sql = """
            SELECT
                id,
                "lobbyPlayerId",
                "questionId",
                points
            FROM
                "LobbyPlayerQuestion"
            WHERE
                "questionId" = :questionId
            """;

        return jdbcClient
                .sql(sql)
                .param("questionId", UUID.fromString(questionId))
                .query(LOBBY_PLAYER_QUESTION_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean updateLobbyPlayerQuestionById(final LobbyPlayerQuestion lobbyPlayerQuestion) {
        String sql = """
            UPDATE "LobbyPlayerQuestion"
            SET
                points = :points,
                "questionId" = :questionId
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("points", lobbyPlayerQuestion.getPoints().orElse(null), Types.INTEGER)
                .param(
                        "questionId",
                        lobbyPlayerQuestion
                                .getQuestionId()
                                .map(UUID::fromString)
                                .orElse(null))
                .param("id", UUID.fromString(lobbyPlayerQuestion.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteLobbyPlayerQuestionByLobbyPlayerId(final String lobbyPlayerId) {
        String sql = """
            DELETE FROM "LobbyPlayerQuestion"
            WHERE "lobbyPlayerId" = :lobbyPlayerId
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("lobbyPlayerId", UUID.fromString(lobbyPlayerId))
                .update();

        return rowsAffected > 0;
    }

    @Override
    public boolean deleteLobbyPlayerQuestionById(final String id) {
        String sql = """
            DELETE FROM "LobbyPlayerQuestion"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();

        return rowsAffected == 1;
    }

    @Override
    public List<String> findUniqueQuestionIdsByLobbyId(final String lobbyId) {
        String sql = """
            SELECT DISTINCT "questionId"
            FROM
                "LobbyPlayerQuestion"
            WHERE
                "lobbyPlayerId" IN (
                    SELECT id FROM "LobbyPlayer" WHERE "lobbyId" = :lobbyId
                )
            AND "questionId" IS NOT NULL
            ORDER BY
                "questionId"
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyId", UUID.fromString(lobbyId))
                .query((rs, rowNum) -> rs.getString("questionId"))
                .list();
    }
}
