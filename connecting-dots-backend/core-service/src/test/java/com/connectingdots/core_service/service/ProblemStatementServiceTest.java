package com.connectingdots.core_service.service;

import com.connectingdots.core_service.dto.ProblemStatementRequest;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProblemStatementServiceTest {

    @Mock
    private ProblemStatementRepository problemStatementRepository;

    @Mock
    private NgoProfileRepository ngoProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProblemStatementService problemStatementService;

    private User mockUser;
    private NgoProfile mockNgoProfile;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("ngo@example.com")
                .role(User.Role.NGO)
                .build();

        mockNgoProfile = NgoProfile.builder()
                .user(mockUser)
                .organizationName("Mock NGO")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser.getEmail(), "password", Collections.emptyList())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateProblemStatementWithUploadedStatusWhenFileIsProvided() {
        ProblemStatementRequest request = new ProblemStatementRequest(
                "Test Title",
                "Test Description",
                "Technology",
                "http://example.com/file.pdf",
                "PDF",
                "UPLOADED"
        );

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(ngoProfileRepository.findByUser(mockUser)).thenReturn(Optional.of(mockNgoProfile));
        when(problemStatementRepository.save(any(ProblemStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProblemStatement result = problemStatementService.createProblemStatement(request);

        assertNotNull(result);
        assertEquals("Test Title", result.getTitle());
        assertEquals("http://example.com/file.pdf", result.getSourceFileUrl());
        assertEquals("PDF", result.getSourceType());
        assertEquals(ProblemStatement.Status.UPLOADED, result.getStatus());
    }

    @Test
    void shouldCreateProblemStatementWithDefaultOpenStatusWhenNoFileAndNoStatus() {
        ProblemStatementRequest request = new ProblemStatementRequest(
                "Test Title",
                "Test Description",
                "Technology",
                null,
                null,
                null
        );

        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));
        when(ngoProfileRepository.findByUser(mockUser)).thenReturn(Optional.of(mockNgoProfile));
        when(problemStatementRepository.save(any(ProblemStatement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProblemStatement result = problemStatementService.createProblemStatement(request);

        assertNotNull(result);
        assertEquals(ProblemStatement.Status.OPEN, result.getStatus());
        assertNull(result.getSourceFileUrl());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotNgo() {
        mockUser.setRole(User.Role.CONTRIBUTOR);
        when(userRepository.findByEmail(mockUser.getEmail())).thenReturn(Optional.of(mockUser));

        ProblemStatementRequest request = new ProblemStatementRequest("T", "D", "Dom", null, null, null);

        assertThrows(ResponseStatusException.class, () -> problemStatementService.createProblemStatement(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByDomainAndStatus() {
        Page<ProblemStatement> mockPage = new PageImpl<>(Collections.emptyList());
        when(problemStatementRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(mockPage);

        Page<ProblemStatement> result = problemStatementService.getProblemStatements("Technology", "OPEN", PageRequest.of(0, 10));

        assertNotNull(result);
        verify(problemStatementRepository, times(1)).findAll(any(Specification.class), any(PageRequest.class));
    }
}
