package com.connectingdots.core_service.config;

import com.connectingdots.core_service.dto.IngestionMessage;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.service.QStashService;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// ==============================================================================
// Startup Seeder: Always verifies the default Admin user is registered.
// Gate dev mock data under the 'seed-data' profile check.
// ==============================================================================
@Component
@RequiredArgsConstructor
public class TestDatabaseSeeder implements CommandLineRunner {

    private final ProblemStatementRepository problemStatementRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final QStashService qStashService;
    private final Environment environment;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        // 0. Ensure Admin User exists safely
        try {
            org.springframework.security.crypto.password.PasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            User adminUser = userRepository.findByEmail("admin@connectingdots.org").orElse(null);
            if (adminUser == null) {
                adminUser = User.builder()
                        .email("admin@connectingdots.org")
                        .passwordHash(encoder.encode("Admin@1234"))
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(adminUser);
                System.out.println("[DATABASE SEEDER] Created Default Admin User: admin@connectingdots.org / Admin@1234");
            } else {
                adminUser.setPasswordHash(encoder.encode("Admin@1234"));
                adminUser.setRole(User.Role.ADMIN);
                adminUser.setActive(true);
                userRepository.save(adminUser);
                System.out.println("[DATABASE SEEDER] Admin user admin@connectingdots.org reset to: Admin@1234");
            }
        } catch (Exception e) {
            System.out.println("[DATABASE SEEDER] Admin user seeding note: " + e.getMessage());
        }

        // Startup Audit: Verify and log all problem statements existing in database
        try {
            long problemCount = problemStatementRepository.count();
            System.out.println("=================================================");
            System.out.println("[DATABASE AUDIT AT STARTUP] Total Problem Statements found in DB: " + problemCount);
            if (problemCount > 0) {
                problemStatementRepository.findAll().forEach(p -> {
                    String ngoName = (p.getNgoProfile() != null) ? p.getNgoProfile().getOrganizationName() : "Unassigned";
                    System.out.println("  -> [ID: " + p.getId() + "] Status: " + p.getStatus() + " | Title: \"" + p.getTitle() + "\" | NGO: " + ngoName);
                });
            } else {
                System.out.println("  -> No problem statements found. Database ready for new submissions.");
            }
            System.out.println("=================================================");
        } catch (Exception auditEx) {
            System.out.println("[DATABASE AUDIT] Audit check failed: " + auditEx.getMessage());
        }

        if (java.util.Arrays.asList(environment.getActiveProfiles()).contains("seed-data")) {
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
}
