package com.smartresolve.config;

import com.smartresolve.entity.SupportTeam;
import com.smartresolve.repository.SupportTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SupportTeamDataInitializer implements CommandLineRunner {

    private final SupportTeamRepository supportTeamRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<SupportTeam> defaults = List.of(
                team(
                        SupportTeamDefaults.DATABASE_SUPPORT_ID,
                        "Database Support",
                        "Handles database connectivity, query and database performance issues."
                ),
                team(
                        SupportTeamDefaults.NETWORK_SUPPORT_ID,
                        "Network Support",
                        "Handles VPN, internet, firewall and network connectivity issues."
                ),
                team(
                        SupportTeamDefaults.SOFTWARE_SUPPORT_ID,
                        "Software Support",
                        "Handles application errors, software installation and configuration issues."
                )
        );

        defaults.forEach(defaultTeam ->
                supportTeamRepository.findById(defaultTeam.getId())
                        .orElseGet(() -> supportTeamRepository.save(defaultTeam)));
    }

    private SupportTeam team(
            java.util.UUID id,
            String name,
            String description
    ) {
        return SupportTeam.builder()
                .id(id)
                .teamName(name)
                .description(description)
                .active(true)
                .build();
    }
}
