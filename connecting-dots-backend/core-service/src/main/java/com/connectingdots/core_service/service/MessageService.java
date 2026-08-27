package com.connectingdots.core_service.service;

import com.connectingdots.core_service.entity.Application;
import com.connectingdots.core_service.entity.Message;
import com.connectingdots.core_service.repository.ApplicationRepository;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.MessageRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final ProblemStatementRepository problemStatementRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final UserRepository userRepository;

    public Message sendMessage(UUID applicationId, UUID senderId, String content) {
        // 1. Fetch application
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // 2. Resolve senderId if not explicitly provided
        UUID actualSenderId = senderId;
        if (actualSenderId == null) {
            actualSenderId = getAuthenticatedUserId();
        }

        // 3. Verify sender authorization (either contributor or NGO tied to application)
        boolean isContributor = isContributorSender(application, actualSenderId);
        boolean isNgo = isNgoSender(application, actualSenderId);

        if (!isContributor && !isNgo) {
            throw new SecurityException("User is not authorized to send messages for this application");
        }

        // 4. Save and return message
        Message message = Message.builder()
                .applicationId(applicationId)
                .senderId(actualSenderId)
                .content(content)
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessagesForApplication(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new RuntimeException("Application not found");
        }
        return messageRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }

    private boolean isContributorSender(Application application, UUID senderId) {
        return contributorProfileRepository.findById(application.getContributorProfileId())
                .map(profile -> profile.getUser())
                .map(user -> user.getId())
                .map(id -> id.equals(senderId))
                .orElse(false);
    }

    private boolean isNgoSender(Application application, UUID senderId) {
        return problemStatementRepository.findById(application.getProblemId())
                .map(problemStatement -> problemStatement.getNgoProfile())
                .map(ngoProfile -> ngoProfile.getUser() != null && ngoProfile.getUser().getId().equals(senderId))
                .orElse(false);
    }

    private UUID getAuthenticatedUserId() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            throw new SecurityException("User is not authenticated");
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
