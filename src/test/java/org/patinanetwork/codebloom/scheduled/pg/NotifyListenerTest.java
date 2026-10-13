package org.patinanetwork.codebloom.scheduled.pg;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.patinanetwork.codebloom.common.db.models.job.Job;
import org.patinanetwork.codebloom.common.db.models.job.JobStatus;
import org.patinanetwork.codebloom.common.db.models.lobby.Lobby;
import org.patinanetwork.codebloom.common.db.models.lobby.LobbyStatus;
import org.patinanetwork.codebloom.common.db.repos.job.JobRepository;
import org.patinanetwork.codebloom.common.db.repos.lobby.LobbyRepository;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.patinanetwork.codebloom.config.NoJdaRequired;
import org.patinanetwork.codebloom.scheduled.pg.handler.JobNotifyHandler;
import org.patinanetwork.codebloom.scheduled.pg.handler.LobbyNotifyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "codebloom.scheduling.enabled=false")
@ActiveProfiles({"ci", "thread"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class NotifyListenerTest extends NoJdaRequired {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    @MockitoBean
    private JobNotifyHandler jobNotifyHandler;

    @MockitoBean
    private LobbyNotifyHandler lobbyNotifyHandler;

    @Test
    void testJobInsertTriggersNotification() throws Exception {
        Job testJob = Job.builder()
                .questionId(UUID.randomUUID().toString())
                .status(JobStatus.INCOMPLETE)
                .nextAttemptAt(StandardizedOffsetDateTime.now().plusMinutes(5))
                .build();

        jobRepository.createJob(testJob);

        verify(jobNotifyHandler, after(2000)).handle(any(String.class));
    }

    @Test
    void testLobbyInsertTriggersNotification() throws Exception {
        Lobby testLobby = Lobby.builder()
                .joinCode("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .playerCount(0)
                .build();

        lobbyRepository.createLobby(testLobby);

        verify(lobbyNotifyHandler, after(2000)).handle(any(String.class));
    }

    @Test
    void testLobbyUpdateTriggersNotification() throws Exception {
        Lobby testLobby = Lobby.builder()
                .joinCode("UPD-" + UUID.randomUUID().toString().substring(0, 8))
                .status(LobbyStatus.AVAILABLE)
                .expiresAt(Optional.of(StandardizedOffsetDateTime.now().plusHours(1)))
                .playerCount(0)
                .build();

        lobbyRepository.createLobby(testLobby);

        testLobby.setStatus(LobbyStatus.ACTIVE);
        testLobby.setPlayerCount(2);
        lobbyRepository.updateLobby(testLobby);

        verify(lobbyNotifyHandler, after(2000).times(2)).handle(any(String.class));
    }

    @Test
    void testMultipleJobInsertsTriggersMultipleNotifications() throws Exception {
        for (int i = 0; i < 3; i++) {
            Job testJob = Job.builder()
                    .questionId(UUID.randomUUID().toString())
                    .status(JobStatus.INCOMPLETE)
                    .nextAttemptAt(StandardizedOffsetDateTime.now().plusMinutes(5 + i))
                    .build();

            jobRepository.createJob(testJob);
        }

        verify(jobNotifyHandler, after(3000).times(3)).handle(any(String.class));
    }
}
