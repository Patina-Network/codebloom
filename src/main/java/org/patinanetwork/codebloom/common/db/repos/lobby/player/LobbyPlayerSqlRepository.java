package org.patinanetwork.codebloom.common.db.repos.lobby.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.lobby.player.LobbyPlayer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LobbyPlayerSqlRepository implements LobbyPlayerRepository {

    private static final RowMapper<LobbyPlayer> LOBBY_PLAYER_ROW_MAPPER = (rs, rowNum) -> LobbyPlayer.builder()
            .id(rs.getString("id"))
            .lobbyId(rs.getString("lobbyId"))
            .playerId(rs.getString("playerId"))
            .points(rs.getInt("points"))
            .build();

    private final JdbcClient jdbcClient;

    public LobbyPlayerSqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createLobbyPlayer(final LobbyPlayer lobbyPlayer) {
        String sql = """
            INSERT INTO "LobbyPlayer"
                (id, "lobbyId", "playerId", points)
            VALUES
                (:id, :lobbyId, :playerId, :points)
            """;

        lobbyPlayer.setId(UUID.randomUUID().toString());

        jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(lobbyPlayer.getId()))
                .param("lobbyId", UUID.fromString(lobbyPlayer.getLobbyId()))
                .param("playerId", UUID.fromString(lobbyPlayer.getPlayerId()))
                .param("points", lobbyPlayer.getPoints())
                .update();
    }

    @Override
    public Optional<LobbyPlayer> findLobbyPlayerById(final String id) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "playerId",
                points
            FROM
                "LobbyPlayer"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(LOBBY_PLAYER_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<LobbyPlayer> findValidLobbyPlayerByPlayerId(final String playerId) {
        String sql = """
            SELECT
                lp.id,
                lp."lobbyId",
                lp."playerId",
                lp.points
            FROM
                "LobbyPlayer" lp
            JOIN
                "Lobby" l
            ON
                l.id = lp."lobbyId"
            WHERE
                lp."playerId" = :playerId
            AND
                (l.status = 'AVAILABLE' OR l.status = 'ACTIVE')
            """;

        return jdbcClient
                .sql(sql)
                .param("playerId", UUID.fromString(playerId))
                .query(LOBBY_PLAYER_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<LobbyPlayer> findPlayersByLobbyId(final String lobbyId) {
        String sql = """
            SELECT
                id,
                "lobbyId",
                "playerId",
                points
            FROM
                "LobbyPlayer"
            WHERE
                "lobbyId" = :lobbyId
            ORDER BY
                points DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("lobbyId", UUID.fromString(lobbyId))
                .query(LOBBY_PLAYER_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean updateLobbyPlayer(final LobbyPlayer lobbyPlayer) {
        String sql = """
            UPDATE "LobbyPlayer"
            SET
                points = :points
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("points", lobbyPlayer.getPoints())
                .param("id", UUID.fromString(lobbyPlayer.getId()))
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deletePlayersByLobbyId(final String lobbyId) {
        String sql = """
            DELETE FROM "LobbyPlayer"
            WHERE "lobbyId" = :lobbyId
            """;

        int rowsAffected =
                jdbcClient.sql(sql).param("lobbyId", UUID.fromString(lobbyId)).update();
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteLobbyPlayerById(final String id) {
        String sql = """
            DELETE FROM "LobbyPlayer"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected == 1;
    }
}
