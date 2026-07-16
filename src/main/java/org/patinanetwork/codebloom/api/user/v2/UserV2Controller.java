package org.patinanetwork.codebloom.api.user.v2;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.patinanetwork.codebloom.common.db.models.user.User;
import org.patinanetwork.codebloom.common.db.repos.question.QuestionRepository;
import org.patinanetwork.codebloom.common.db.repos.user.UserRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.patinanetwork.codebloom.common.dto.autogen.UnsafeGenericFailureResponse;
import org.patinanetwork.codebloom.common.dto.user.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(
        name = "General user routes (v2)",
        description =
                "This controller is responsible for handling general user data, such as user profile, user submissions, and more.")
@RequestMapping("/api/user/v2")
@Timed(value = "controller.execution")
public class UserV2Controller {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public UserV2Controller(final QuestionRepository questionRepository, final UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Operation(
            summary = "Public route that returns the given user's profile",
            description = """
            Unprotected endpoint that returns the user profile of the LeetCode username that is passed to the endpoint's path.
        """,
            responses = {
                @ApiResponse(
                        responseCode = "404",
                        description = "User profile has not been found",
                        content = @Content(schema = @Schema(implementation = UnsafeGenericFailureResponse.class))),
                @ApiResponse(responseCode = "200", description = "User profile has been found"),
            })
    @GetMapping("{leetcodeUsername}/profile")
    public ResponseEntity<ApiResponder<UserDto>> getUserProfileByLeetcodeUsername(
            final HttpServletRequest request, @PathVariable final String leetcodeUsername) {

        User user = userRepository.getUserByLeetcodeUsername(leetcodeUsername);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Failed to find user profile.");
        }

        return ResponseEntity.ok().body(ApiResponder.success("User profile found!", UserDto.fromUser(user)));
    }
}
