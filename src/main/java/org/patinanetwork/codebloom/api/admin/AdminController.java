package org.patinanetwork.codebloom.api.admin;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.patinanetwork.codebloom.api.admin.body.*;
import org.patinanetwork.codebloom.api.admin.body.jda.DeleteMessageBody;
import org.patinanetwork.codebloom.common.components.DiscordClubManager;
import org.patinanetwork.codebloom.common.components.LeaderboardManager;
import org.patinanetwork.codebloom.common.db.models.announcement.Announcement;
import org.patinanetwork.codebloom.common.db.models.discord.DiscordClub;
import org.patinanetwork.codebloom.common.db.models.leaderboard.Leaderboard;
import org.patinanetwork.codebloom.common.db.models.question.QuestionWithUser;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.announcement.AnnouncementRepository;
import org.patinanetwork.codebloom.common.db.repos.discord.club.DiscordClubRepository;
import org.patinanetwork.codebloom.common.db.repos.leaderboard.LeaderboardRepository;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.Empty;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.question.QuestionWithUserDto;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.patinanetwork.codebloom.common.security.AuthenticationObject;
import org.patinanetwork.codebloom.common.security.annotation.Protected;
import org.patinanetwork.codebloom.common.time.StandardizedOffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "Admin routes", description = "This controller is responsible for handling all admin routes.")
@RequestMapping("/api/admin")
@Timed(value = "controller.execution")
public class AdminController {

    private final UserRepository userRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final AnnouncementRepository announcementRepository;
    private final QuestionRepository questionRepository;
    private final DiscordClubManager discordClubManager;
    private final LeaderboardManager leaderboardManager;
    private final DiscordClubRepository discordClubRepository;

    public AdminController(
            final LeaderboardRepository leaderboardRepository,
            final UserRepository userRepository,
            final AnnouncementRepository announcementRepository,
            final QuestionRepository questionRepository,
            final DiscordClubManager discordClubManager,
            final LeaderboardManager leaderboardManager,
            final DiscordClubRepository discordClubRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
        this.announcementRepository = announcementRepository;
        this.questionRepository = questionRepository;
        this.discordClubManager = discordClubManager;
        this.leaderboardManager = leaderboardManager;
        this.discordClubRepository = discordClubRepository;
    }

    @Operation(summary = "Drops current leaderboard and add new one", description = """
            BE SUPER CAREFUL WITH THIS ROUTE!!!!!!! It will drop the current leaderboard and add a new leaderboard based on the given parameters.
        """)
    @PostMapping("/leaderboard/create")
    public ResponseEntity<ApiResponder<Empty>> createLeaderboard(
            @Protected(admin = true) final AuthenticationObject authenticationObject,
            @Valid @RequestBody final NewLeaderboardBody newLeaderboardBody) {

        final String name = newLeaderboardBody.getName().trim();

        if (name.isEmpty() || name.length() > 512) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponder.failure("Leaderboard name must be between 1 and 512 characters."));
        }

        // BE VERY CAREFUL WITH THIS ROUTE. IT WILL DEACTIVATE THE PREVIOUS LEADERBOARD
        // (however, it should be in a recoverable state, as it just gets toggled to be
        // deactivated, not deleted).
        Optional<Leaderboard> currentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();

        currentLeaderboard.ifPresent(lb -> {
            discordClubManager.sendLeaderboardCompletedDiscordMessageToAllClubs();
            leaderboardManager.generateAchievementsForAllWinners();
            leaderboardRepository.disableLeaderboardById(lb.getId());
        });

        OffsetDateTime shouldExpireBy = StandardizedOffsetDateTime.normalize(newLeaderboardBody.getShouldExpireBy());

        if (shouldExpireBy != null) {
            OffsetDateTime now = StandardizedOffsetDateTime.now();
            if (!now.isBefore(shouldExpireBy)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponder.failure("The expiration date must be in the future."));
            }
        }

        Leaderboard newLeaderboard = Leaderboard.builder()
                .name(name)
                .shouldExpireBy(Optional.ofNullable(shouldExpireBy).map(d -> d.toLocalDateTime()))
                .syntaxHighlightingLanguage(Optional.ofNullable(newLeaderboardBody.getSyntaxHighlightingLanguage()))
                .build();

        leaderboardRepository.addNewLeaderboard(newLeaderboard);
        leaderboardRepository.addAllUsersToLeaderboard(newLeaderboard.getId());

        return ResponseEntity.ok(ApiResponder.success("Leaderboard was created successfully.", Empty.of()));
    }

    @Operation(summary = "Allows current admin to toggle another user's admin status", description = """
        """)
    @PostMapping("/user/admin/toggle")
    public ResponseEntity<ApiResponder<UserDto>> updateAdmin(
            @Protected(admin = true) final AuthenticationObject authenticationObject,
            @Valid @RequestBody final UpdateAdminBody newAdminBody) {

        final String userId = newAdminBody.getId();
        final boolean toggleTo = newAdminBody.getToggleTo();

        var userResult = userRepository.getUserById(userId);
        if (userResult.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponder.failure("User has not been found."));
        }
        User user = userResult.get();

        user.setAdmin(toggleTo);
        boolean isSuccessful = userRepository.updateUser(user);

        if (!isSuccessful) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponder.failure("Failed to update the admin."));
        }

        return ResponseEntity.ok(ApiResponder.success(
                "User with Discord name of " + user.getDiscordName()
                        + " is "
                        + (toggleTo ? "now an admin!" : "no longer an admin."),
                UserDto.fromUser(user)));
    }

    @Operation(
            summary = "Create a new announcement (only for admins).",
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "Announcement successfully created"),
                @ApiResponse(
                        responseCode = "500",
                        description = "Something went wrong",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @PostMapping("/announcement/create")
    public ResponseEntity<ApiResponder<Announcement>> createNewAnnouncement(
            @Valid @RequestBody final CreateAnnouncementBody createAnnouncementBody,
            @Protected(admin = true) final AuthenticationObject authenticationObject) {

        OffsetDateTime nowWithOffset = StandardizedOffsetDateTime.now();
        OffsetDateTime expiresAtWithOffset =
                StandardizedOffsetDateTime.normalize(createAnnouncementBody.getExpiresAt());
        boolean isInFuture = nowWithOffset.isBefore(expiresAtWithOffset);

        if (!isInFuture) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The expiration date must be in the future.");
        }

        Announcement announcement = Announcement.builder()
                .expiresAt(expiresAtWithOffset)
                .showTimer(Optional.of(createAnnouncementBody.isShowTimer()))
                .message(createAnnouncementBody.getMessage())
                .createdAt(nowWithOffset)
                .build();

        boolean isSuccessful = announcementRepository.createAnnouncement(announcement);

        if (!isSuccessful) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponder.failure("Hmm, something went wrong."));
        }

        return ResponseEntity.ok(ApiResponder.success("New announcement successfully created!", announcement));
    }

    @Operation(
            summary = "Create a delete announcement if exist (only for admins).",
            responses = {
                @ApiResponse(
                        responseCode = "401",
                        description = "Not authenticated",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "Announcement successfully Deleted"),
                @ApiResponse(
                        responseCode = "500",
                        description = "Something went wrong",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @PostMapping("/announcement/disable")
    public ResponseEntity<ApiResponder<Empty>> deleteAnnouncement(
            @Valid @RequestBody final DeleteAnnouncementBody deleteAnnouncementBody,
            @Protected(admin = true) final AuthenticationObject authenticationObject) {
        Announcement announcement = announcementRepository
                .getAnnouncementById(deleteAnnouncementBody.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement does not exist"));
        OffsetDateTime nowWithOffset = StandardizedOffsetDateTime.now();
        announcement.setExpiresAt(nowWithOffset);
        boolean updatedAnnouncement = announcementRepository.updateAnnouncement(announcement);

        if (!updatedAnnouncement) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponder.failure("Hmm, something went wrong."));
        }
        return ResponseEntity.ok(ApiResponder.success("Announcement successfully disabled!", Empty.of()));
    }

    @Operation(
            summary = "Get all incomplete questions with user information",
            description = """
        Returns all questions that are missing runtime, memory, code, or language information,
        ordered by most recently submitted. Only accessible to admins.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "Retrieved incomplete questions"),
                @ApiResponse(
                        responseCode = "404",
                        description = "No Incomplete Questions",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @GetMapping("/questions/incomplete")
    public ResponseEntity<ApiResponder<List<QuestionWithUserDto>>> getIncompleteQuestions(
            @Protected(admin = true) final AuthenticationObject authenticationObject) {

        ArrayList<QuestionWithUser> incompleteQuestions = questionRepository.getAllIncompleteQuestionsWithUser();

        if (incompleteQuestions.size() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Incomplete Questions");
        }

        List<QuestionWithUserDto> incompleteQuestionsDto = incompleteQuestions.stream()
                .map(QuestionWithUserDto::fromQuestionWithUser)
                .toList();

        return ResponseEntity.ok(ApiResponder.success(
                "Retrieved " + incompleteQuestionsDto.size() + " incomplete questions.", incompleteQuestionsDto));
    }

    @Operation(
            summary = "Send a message to a club's discord",
            description = """
        Sends an embedded message to the leaderboard channel of the guild associated with their club, provided its clubId. Only accessible to admins.
        """,
            responses = {
                @ApiResponse(responseCode = "200", description = "Message was sent successfully"),
                @ApiResponse(
                        responseCode = "404",
                        description = "Something went wrong",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
            })
    @PostMapping("/discord/message/test")
    public ResponseEntity<ApiResponder<Empty>> sendDiscordMessage(
            @RequestBody final String clubId,
            @Protected(admin = true) final AuthenticationObject authenticationObject) {

        Optional<DiscordClub> clubOpt = discordClubRepository.getDiscordClubById(clubId);
        if (clubOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponder.failure("Club not found."));
        }
        DiscordClub club = clubOpt.get();

        boolean sentMessage = discordClubManager.sendTestEmbedMessageToClub(club);

        if (!sentMessage) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponder.failure("Hmm, something went wrong."));
        }
        return ResponseEntity.ok(ApiResponder.success("Message successfully sent!", Empty.of()));
    }

    @DeleteMapping("/discord/message")
    public ResponseEntity<ApiResponder<Empty>> deleteDiscordMessage(
            @Valid @RequestBody final DeleteMessageBody deleteMessageBody,
            @Protected(admin = true) final AuthenticationObject authenticationObject) {

        boolean isDeleted = discordClubManager.deleteMessageById(
                deleteMessageBody.getChannelId(), deleteMessageBody.getMessageId());

        if (!isDeleted) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponder.failure("Hmm, something went wrong."));
        }

        return ResponseEntity.ok(ApiResponder.success("Discord Message successfully deleted", Empty.of()));
    }

    @Operation(summary = "Edit current leaderboard")
    @PutMapping("/leaderboard/current")
    public ResponseEntity<ApiResponder<Empty>> editCurrentLeaderboard(
            @RequestBody final EditLeaderboardBody editLeaderboardBody,
            @Protected(admin = true) final AuthenticationObject authenticationObject) {
        editLeaderboardBody.validate();

        Optional<Leaderboard> currentLeaderboard = leaderboardRepository.getRecentLeaderboardMetadata();
        currentLeaderboard.ifPresent(lb -> {
            OffsetDateTime shouldExpireBy =
                    StandardizedOffsetDateTime.normalize(editLeaderboardBody.getShouldExpireBy());

            Leaderboard updated = Leaderboard.builder()
                    .name(editLeaderboardBody.getName())
                    .deletedAt(lb.getDeletedAt())
                    .createdAt(lb.getCreatedAt())
                    .shouldExpireBy(Optional.ofNullable(shouldExpireBy).map(d -> d.toLocalDateTime()))
                    .syntaxHighlightingLanguage(
                            Optional.ofNullable(editLeaderboardBody.getSyntaxHighlightingLanguage()))
                    .id(lb.getId())
                    .build();

            leaderboardRepository.updateLeaderboard(updated);
        });

        if (currentLeaderboard.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No current leaderboard found");
        }

        return ResponseEntity.ok().body(ApiResponder.success("Leaderboard updated successfully", Empty.of()));
    }
}
