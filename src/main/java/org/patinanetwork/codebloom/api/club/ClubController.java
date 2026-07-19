package org.patinanetwork.codebloom.api.club;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.patinanetwork.codebloom.api.admin.body.RegisterClubBody;
import org.patinanetwork.codebloom.api.club.dto.ClubDto;
import org.patinanetwork.codebloom.api.club.service.ClubService;
import org.patinanetwork.codebloom.common.db.models.club.Club;
import org.patinanetwork.codebloom.common.db.models.usertag.UserTag;
import org.patinanetwork.codebloom.common.db.repos.club.ClubRepository;
import org.patinanetwork.codebloom.common.db.repos.usertag.UserTagRepository;
import org.patinanetwork.codebloom.common.dto.ApiResponder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/club")
@Tag(name = "General Club Routes")
@Timed(value = "controller.execution")
public class ClubController {

    private final ClubService clubService;
    private final ClubRepository clubRepository;
    private final UserTagRepository userTagRepository;

    public ClubController(
            final ClubService clubService,
            final ClubRepository clubRepository,
            final UserTagRepository userTagRepository) {
        this.clubService = clubService;
        this.clubRepository = clubRepository;
        this.userTagRepository = userTagRepository;
    }

    @GetMapping("/{clubSlug}")
    public ResponseEntity<ApiResponder<ClubDto>> getClubDataBySlug(@PathVariable final String clubSlug) {

        ClubDto club = clubService.getClubDtoBySlug(clubSlug);

        if (club == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Club does not exist.");
        }

        return ResponseEntity.ok().body(ApiResponder.success("Club Found", club));
    }

    @Operation(summary = "Gives user a club tag if the password is correct")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponder<UserTag>> validatePassword(
            @RequestBody final RegisterClubBody registerClubBody) {
        final String userId = registerClubBody.getUserId();
        final String clubSlug = registerClubBody.getClubSlug();
        final String password = registerClubBody.getPassword();

        Club club = clubRepository.getClubBySlug(clubSlug);

        if (club == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Club does not exist.");
        }

        boolean valid = clubService.isPasswordValid(club, password);

        if (!valid) {
            return ResponseEntity.ok(ApiResponder.custom(false, "Incorrect Password", null));
        }

        UserTag clubTag = UserTag.builder().userId(userId).tag(club.getTag()).build();

        userTagRepository.createTag(clubTag);
        return ResponseEntity.ok(ApiResponder.success("Club Tag added.", clubTag));
    }
}
