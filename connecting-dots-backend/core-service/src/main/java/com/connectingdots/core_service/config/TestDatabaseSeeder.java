package com.connectingdots.core_service.config;

import com.connectingdots.core_service.dto.IngestionMessage;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.service.QStashService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDatabaseSeeder implements CommandLineRunner {

    private final ProblemStatementRepository problemStatementRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final QStashService qStashService;

    @Override
    public void run(String... args) throws Exception {
        // 0. Ensure Admin User exists safely
        try {
            if (!userRepository.existsByEmail("admin@connectingdots.org")) {
                org.springframework.security.crypto.password.PasswordEncoder encoder = 
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
                User adminUser = User.builder()
                        .email("admin@connectingdots.org")
                        .passwordHash(encoder.encode("Admin@1234"))
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(adminUser);
                System.out.println("[DATABASE SEEDER] Created Default Admin User: admin@connectingdots.org / Admin@1234");
            }
        } catch (Exception e) {
            System.out.println("[DATABASE SEEDER] Admin user already exists or seeding skipped: " + e.getMessage());
        }

        ProblemStatement problemStatement = problemStatementRepository.findAll().stream().findFirst().orElseGet(() -> {

            // 1. Ensure User exists to avoid unique constraint violations on email
            User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
                User newUser = User.builder()
                        .email("test_webhook_ngo@example.com")
                        .passwordHash("dummy_hash")
                        .role(User.Role.NGO)
                        .isActive(true)
                        .build();
                return userRepository.save(newUser);
            });

            // 2. Ensure NgoProfile exists since it's required for ProblemStatement
            NgoProfile ngoProfile = ngoProfileRepository.findAll().stream().findFirst().orElseGet(() -> {
                NgoProfile newNgoProfile = NgoProfile.builder()
                        .user(user)
                        .organizationName("QStash Test NGO")
                        .domain("Technology")
                        .contactNumber("1234567890")
                        .build();
                return ngoProfileRepository.save(newNgoProfile);
            });

            // 3. Create the ProblemStatement with the requested dummy data
            ProblemStatement newProblemStatement = ProblemStatement.builder()
                    .title("QStash Async Integration Test")
                    .description("Testing the webhook pipeline")
                    .status(ProblemStatement.Status.OPEN)
                    .sourceFileUrl("https://res.cloudinary.com/demo/raw/upload/dummy_document.pdf")
                    .sourceType("PDF")
                    .domain("Data Science")
                    .ngoProfile(ngoProfile)
                    .build();

            return problemStatementRepository.save(newProblemStatement);
        });

        // Ensure the record has valid webhook payload fields
        if (problemStatement.getSourceFileUrl() == null || problemStatement.getSourceType() == null) {
            problemStatement.setSourceFileUrl("https://res.cloudinary.com/demo/raw/upload/dummy_document.pdf");
            problemStatement.setSourceType("PDF");
            problemStatement = problemStatementRepository.save(problemStatement);
        }

        // 4. Construct the IngestionMessage
        IngestionMessage message = new IngestionMessage(
                problemStatement.getId(),
                problemStatement.getSourceFileUrl(),
                problemStatement.getSourceType()
        );

        // 5. Dispatch the webhook
        qStashService.publishToAiService(message);

        // 6. Log the UUID clearly to the console
        System.out.println("=================================================");
        System.out.println("[AUTOMATED TEST] QStash Webhook Fired for UUID: " + problemStatement.getId());
        System.out.println("=================================================");
    }
}
