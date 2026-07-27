package org.patinanetwork.codebloom.common.db.repos.lobby;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LobbySqlRepository implements LobbyRepository {

    private static final RowMapper<Lobby> LOBBY_ROW_MAPPER = (rs, rowNum) -> Lobby.builder()
            .id(rs.getString("id"))
            .joinCode(rs.getString("joinCode"))
            .status(LobbyStatus.valueOf(rs.getString("status")))
            .createdAt(rs.getObject("createdAt", OffsetDateTime.class))
            .expiresAt(Optional.ofNullable(rs.getObject("expiresAt", OffsetDateTime.class)))
            .playerCount(rs.getInt("playerCount"))
            .winnerId(Optional.ofNullable(rs.getString("winnerId")))
            .tie(rs.getBoolean("tie"))
            .build();

    private final JdbcClient jdbcClient;

    public LobbySqlRepository(final JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createLobby(final Lobby lobby) {
        String sql = """
            INSERT INTO "Lobby"
                (id, "joinCode", status, "expiresAt", "playerCount", "winnerId", "tie")
            VALUES
                (:id, :joinCode, :status, :expiresAt, :playerCount, :winnerId, :tie)
            RETURNING
                "createdAt"
            """;

        lobby.setId(UUID.randomUUID().toString());

        OffsetDateTime createdAt = jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(lobby.getId()))
                .param("joinCode", lobby.getJoinCode())
                .param("status", lobby.getStatus().name(), Types.OTHER)
                .param(
                        "expiresAt",
                        lobby.getExpiresAt()
                                .map(StandardizedOffsetDateTime::normalize)
                                .orElse(null))
                .param("playerCount", lobby.getPlayerCount())
                .param("winnerId", lobby.getWinnerId().map(UUID::fromString).orElse(null))
                .param("tie", lobby.isTie())
                .query((rs, rowNum) -> rs.getObject("createdAt", OffsetDateTime.class))
                .optional()
                .orElse(null);

        lobby.setCreatedAt(createdAt);
    }

    @Override
    public Optional<Lobby> findLobbyById(final String id) {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                id = :id
            """;

        return jdbcClient
                .sql(sql)
                .param("id", UUID.fromString(id))
                .query(LOBBY_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<Lobby> findAvailableLobbyByJoinCode(final String joinCode) {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                "joinCode" = :joinCode
                AND status = :status
            """;

        return jdbcClient
                .sql(sql)
                .param("joinCode", joinCode)
                .param("status", LobbyStatus.AVAILABLE.name(), Types.OTHER)
                .query(LOBBY_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<Lobby> findActiveLobbyByJoinCode(final String joinCode) {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                "joinCode" = :joinCode
                AND status = 'ACTIVE'
            """;

        return jdbcClient
                .sql(sql)
                .param("joinCode", joinCode)
                .query(LOBBY_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<Lobby> findLobbiesByStatus(final LobbyStatus status) {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                status = :status
            ORDER BY
                "createdAt" DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("status", status.name(), Types.OTHER)
                .query(LOBBY_ROW_MAPPER)
                .list();
    }

    @Override
    public List<Lobby> findAvailableLobbies() {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                status = :status
                AND "expiresAt" > NOW()
            ORDER BY
                "createdAt" DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("status", LobbyStatus.AVAILABLE.name(), Types.OTHER)
                .query(LOBBY_ROW_MAPPER)
                .list();
    }

    @Override
    public List<Lobby> findActiveLobbies() {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                status = :status
                AND "expiresAt" > NOW()
            ORDER BY
                "createdAt" DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("status", LobbyStatus.ACTIVE.name(), Types.OTHER)
                .query(LOBBY_ROW_MAPPER)
                .list();
    }

    @Override
    public List<Lobby> findExpiredLobbies() {
        String sql = """
            SELECT
                id,
                "joinCode",
                status,
                "createdAt",
                "expiresAt",
                "playerCount",
                "winnerId",
                "tie"
            FROM
                "Lobby"
            WHERE
                status = :status
                AND "expiresAt" <= NOW()
            ORDER BY
                "createdAt" DESC
            """;

        return jdbcClient
                .sql(sql)
                .param("status", LobbyStatus.ACTIVE.name(), Types.OTHER)
                .query(LOBBY_ROW_MAPPER)
                .list();
    }

    @Override
    public Optional<Lobby> findActiveLobbyByLobbyPlayerPlayerId(final String lobbyPlayerId) {
        String sql = """
            SELECT
                l.id,
                l."joinCode",
                l.status,
                l."createdAt",
                l."expiresAt",
                l."playerCount",
                l."winnerId",
                l."tie"
            FROM
                "Lobby" l
            INNER JOIN
                "LobbyPlayer" lp ON l.id = lp."lobbyId"
            WHERE
                l.status = :status
                AND lp."playerId" = :playerId
            """;

        return jdbcClient
                .sql(sql)
                .param("status", LobbyStatus.ACTIVE.name(), Types.OTHER)
                .param("playerId", UUID.fromString(lobbyPlayerId))
                .query(LOBBY_ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<Lobby> findAvailableLobbyByLobbyPlayerPlayerId(final String lobbyPlayerId) {
        String sql = """
            SELECT
                l.id,
                l."joinCode",
                l.status,
                l."createdAt",
                l."expiresAt",
                l."playerCount",
                l."winnerId",
                l."tie"
            FROM
                "Lobby" l
            JOIN
                "LobbyPlayer" lp ON l.id = lp."lobbyId"
            WHERE
                l.status = 'AVAILABLE'
            AND
                lp."playerId" = :playerId
            """;

        return jdbcClient
                .sql(sql)
                .param("playerId", UUID.fromString(lobbyPlayerId))
                .query(LOBBY_ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean updateLobby(final Lobby lobby) {
        String sql = """
            UPDATE "Lobby"
            SET
                status = :status,
                "playerCount" = :playerCount,
                "winnerId" = :winnerId,
                "tie" = :tie,
                "expiresAt" = :expiresAt
            WHERE
                id = :id
            """;

        int rowsAffected = jdbcClient
                .sql(sql)
                .param("status", lobby.getStatus().name(), Types.OTHER)
                .param("playerCount", lobby.getPlayerCount())
                .param("id", UUID.fromString(lobby.getId()))
                .param("winnerId", lobby.getWinnerId().map(UUID::fromString).orElse(null))
                .param("tie", lobby.isTie())
                .param(
                        "expiresAt",
                        lobby.getExpiresAt().isPresent()
                                ? StandardizedOffsetDateTime.normalize(
                                        lobby.getExpiresAt().get())
                                : null)
                .update();

        return rowsAffected == 1;
    }

    @Override
    public boolean deleteLobbyById(final String id) {
        String sql = """
            DELETE FROM "Lobby"
            WHERE id = :id
            """;

        int rowsAffected = jdbcClient.sql(sql).param("id", UUID.fromString(id)).update();
        return rowsAffected == 1;
    }
}
