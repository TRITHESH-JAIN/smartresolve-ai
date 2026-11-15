package com.smartresolve.controller;

import com.smartresolve.dto.response.SupportEngineerResponse;
import com.smartresolve.dto.response.SupportTeamResponse;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SupportDirectoryController {

    private final SupportTeamRepository supportTeamRepository;
    private final UserRepository userRepository;

    @GetMapping("/teams")
    public ResponseEntity<List<SupportTeamResponse>> getTeams() {
        return ResponseEntity.ok(
                supportTeamRepository.findAll()
                        .stream()
                        .filter(team -> team.isActive())
                        .sorted((left, right) ->
                                left.getTeamName().compareToIgnoreCase(
                                        right.getTeamName()))
                        .map(team -> new SupportTeamResponse(
                                team.getId(),
                                team.getTeamName(),
                                team.getDescription(),
                                team.isActive()
                        ))
                        .toList()
        );
    }

    @GetMapping("/engineers")
    public ResponseEntity<List<SupportEngineerResponse>> getEngineers() {
        List<User> engineers = userRepository
                .findByRoleAndActiveTrueOrderByFullNameAsc(
                        Role.SUPPORT_ENGINEER);
        return ResponseEntity.ok(toEngineerResponses(engineers));
    }

    @GetMapping("/teams/{teamId}/engineers")
    public ResponseEntity<List<SupportEngineerResponse>> getEngineersByTeam(
            @PathVariable UUID teamId
    ) {
        List<User> engineers = userRepository
                .findByRoleAndActiveTrueAndTeam_IdOrderByFullNameAsc(
                        Role.SUPPORT_ENGINEER,
                        teamId);
        return ResponseEntity.ok(toEngineerResponses(engineers));
    }

    private List<SupportEngineerResponse> toEngineerResponses(
            List<User> engineers
    ) {
        return engineers.stream()
                .map(engineer -> new SupportEngineerResponse(
                        engineer.getId(),
                        engineer.getFullName(),
                        engineer.getEmail(),
                        engineer.getTeam() == null
                                ? null : engineer.getTeam().getId(),
                        engineer.getTeam() == null
                                ? null : engineer.getTeam().getTeamName()
                ))
                .toList();
    }
}
