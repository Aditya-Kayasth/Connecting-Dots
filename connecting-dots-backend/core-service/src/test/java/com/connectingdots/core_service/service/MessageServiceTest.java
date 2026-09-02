package com.connectingdots.core_service.service;

import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.Message;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.MessageRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ProblemStatementRepository problemStatementRepository;
    @Mock private ContributorProfileRepository contributorProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private MessageService messageService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendMessage_ContributorSuccess() {
        UUID applicationId = UUID.randomUUID();
        UUID contributorUserId = UUID.randomUUID();
        UUID contributorProfileId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        Application application = Application.builder()
                .id(applicationId)
                .problemId(problemId)
                .contributorProfileId(contributorProfileId)
                .build();

        User contributorUser = User.builder().email("contributor@test.com").role(User.Role.CONTRIBUTOR).build();
        contributorUser.setId(contributorUserId);

        ContributorProfile contributorProfile = ContributorProfile.builder().user(contributorUser).build();
        ProblemStatement problemStatement = ProblemStatement.builder().ngoProfile(new NgoProfile()).build();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(contributorProfile));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(messageRepository.save(any(Message.class)))
                .thenReturn(Message.builder().content("Hello world").build());

        Message result = messageService.sendMessage(applicationId, contributorUserId, "Hello world");

        assertThat(result).isNotNull();
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void sendMessage_UnauthorizedUserFails() {
        UUID applicationId = UUID.randomUUID();
        UUID unauthorizedUserId = UUID.randomUUID();
        UUID contributorProfileId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        Application application = Application.builder()
                .id(applicationId)
                .problemId(problemId)
                .contributorProfileId(contributorProfileId)
                .build();

        User contributorUser = User.builder().email("contributor@test.com").build();
        contributorUser.setId(UUID.randomUUID());

        ContributorProfile contributorProfile = ContributorProfile.builder().user(contributorUser).build();
        ProblemStatement problemStatement = ProblemStatement.builder().ngoProfile(new NgoProfile()).build();

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(contributorProfileRepository.findById(contributorProfileId)).thenReturn(Optional.of(contributorProfile));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));

        assertThrows(SecurityException.class, () ->
                messageService.sendMessage(applicationId, unauthorizedUserId, "Hello world")
        );

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void getMessagesForApplication_Success() {
        UUID applicationId = UUID.randomUUID();
        UUID ngoUserId = UUID.randomUUID();
        UUID problemId = UUID.randomUUID();

        User ngoUser = User.builder().email("ngo@test.com").role(User.Role.NGO).build();
        ngoUser.setId(ngoUserId);

        NgoProfile ngoProfile = NgoProfile.builder().user(ngoUser).build();
        ProblemStatement problemStatement = ProblemStatement.builder().ngoProfile(ngoProfile).build();

        Application application = Application.builder()
                .id(applicationId)
                .problemId(problemId)
                .contributorProfileId(UUID.randomUUID())
                .build();

        // Mock Security Authentication Context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ngoUser.getEmail(), "password", Collections.emptyList())
        );

        when(userRepository.findByEmail(ngoUser.getEmail())).thenReturn(Optional.of(ngoUser));
        when(userRepository.findById(ngoUserId)).thenReturn(Optional.of(ngoUser));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(problemStatementRepository.findById(problemId)).thenReturn(Optional.of(problemStatement));
        when(messageRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId))
                .thenReturn(List.of(Message.builder().content("Message 1").build()));

        List<Message> messages = messageService.getMessagesForApplication(applicationId);

        assertThat(messages).hasSize(1);
        verify(messageRepository, times(1)).findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }
}
