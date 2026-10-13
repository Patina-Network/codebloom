package org.patinanetwork.codebloom.api.club.service;

import org.patinanetwork.codebloom.api.club.dto.ClubDto;
import org.patinanetwork.codebloom.common.db.models.club.Club;
import org.patinanetwork.codebloom.common.db.repos.club.ClubRepository;
import org.springframework.stereotype.Service;

@Service
public class ClubService {

    private final ClubRepository clubRepository;

    public ClubService(final ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    public ClubDto getClubDtoBySlug(final String slug) {
        Club club = clubRepository.getClubBySlug(slug).orElse(null);
        if (club == null) {
            return null;
        }
        return ClubDto.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .slug(club.getSlug())
                .splashIconUrl(club.getSplashIconUrl())
                .tag(club.getTag())
                .build();
    }

    public boolean isPasswordValid(final Club club, final String rawPassword) {
        return club.getPassword().equals(rawPassword);
    }

    public Club getClubBySlug(final String slug) {
        return clubRepository.getClubBySlug(slug).orElse(null);
    }
}
