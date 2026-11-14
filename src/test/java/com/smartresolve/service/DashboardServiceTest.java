package com.smartresolve.service;

import com.smartresolve.dto.response.DashboardResponse;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.repository.TicketRepository;
import com.smartresolve.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyDashboardForAdmin() {
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .id(adminId)
                .email("admin@smartresolve.com")
                .role(Role.ADMIN)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@smartresolve.com", null, List.of())
        );
        when(userRepository.findByEmail("admin@smartresolve.com"))
                .thenReturn(Optional.of(admin));
        when(ticketRepository.findAll()).thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0, response.totalTickets());
        assertEquals(0, response.openTickets());
        assertEquals(0, response.resolvedTickets());
        assertEquals(0, response.criticalTickets());
        verify(ticketRepository).findAll();
    }

    @Test
    void shouldReturnEmployeeDashboard() {
        UUID employeeId = UUID.randomUUID();
        User employee = User.builder()
                .id(employeeId)
                .email("employee@smartresolve.com")
                .role(Role.EMPLOYEE)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "employee@smartresolve.com", null, List.of())
        );
        when(userRepository.findByEmail("employee@smartresolve.com"))
                .thenReturn(Optional.of(employee));
        when(ticketRepository.findByEmployee_IdOrderByCreatedAtDesc(employeeId))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.getDashboard();

        assertEquals(0, response.totalTickets());
        verify(ticketRepository)
                .findByEmployee_IdOrderByCreatedAtDesc(employeeId);
        verify(ticketRepository, never()).findAll();
    }
}
