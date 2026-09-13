package com.connectingdots.core_service.config;

import com.connectingdots.core_service.dto.IngestionMessage;
import com.connectingdots.core_service.entity.ContributorProfile;
import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import com.connectingdots.core_service.repository.ContributorProfileRepository;
import com.connectingdots.core_service.repository.NgoProfileRepository;
import com.connectingdots.core_service.repository.ProblemStatementRepository;
import com.connectingdots.core_service.repository.UserRepository;
import com.connectingdots.core_service.service.QStashService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// ==============================================================================
// Startup Seeder: Guarantees Admin, Test NGO, and Test Contributor are always
// seeded and available even if the database is truncated.
// ==============================================================================
@Component
@RequiredArgsConstructor
public class TestDatabaseSeeder implements CommandLineRunner {

    private final ProblemStatementRepository problemStatementRepository;
    private final UserRepository userRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final ContributorProfileRepository contributorProfileRepository;
    private final QStashService qStashService;
    private final Environment environment;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        // 1. Seed / Restore Default Admin User
        try {
            User adminUser = userRepository.findByEmail("admin@connectingdots.org").orElse(null);
            if (adminUser == null) {
                adminUser = User.builder()
                        .email("admin@connectingdots.org")
                        .passwordHash(encoder.encode("Admin@1234"))
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.saveAndFlush(adminUser);
                System.out.println("[DATABASE SEEDER] Created Admin User: admin@connectingdots.org / Admin@1234");
            } else {
                adminUser.setPasswordHash(encoder.encode("Admin@1234"));
                adminUser.setRole(User.Role.ADMIN);
                adminUser.setActive(true);
                userRepository.saveAndFlush(adminUser);
                System.out.println("[DATABASE SEEDER] Restored Admin User: admin@connectingdots.org / Admin@1234");
            }
        } catch (Exception e) {
            System.out.println("[DATABASE SEEDER] Admin user seeding note: " + e.getMessage());
        }

        // 2. Seed / Restore Default Test NGO User & Profile
        try {
            User ngoUser = userRepository.findByEmail("ngo_test@connectingdots.org").orElse(null);
            if (ngoUser == null) {
                ngoUser = User.builder()
                        .email("ngo_test@connectingdots.org")
                        .passwordHash(encoder.encode("password123"))
                        .role(User.Role.NGO)
                        .isActive(true)
                        .build();
                ngoUser = userRepository.saveAndFlush(ngoUser);
                System.out.println("[DATABASE SEEDER] Created Test NGO User: ngo_test@connectingdots.org / password123");
            } else {
                ngoUser.setPasswordHash(encoder.encode("password123"));
                ngoUser.setRole(User.Role.NGO);
                ngoUser.setActive(true);
                ngoUser = userRepository.saveAndFlush(ngoUser);
                System.out.println("[DATABASE SEEDER] Restored Test NGO User: ngo_test@connectingdots.org / password123");
            }

            if (!ngoProfileRepository.findByUser(ngoUser).isPresent()) {
                NgoProfile ngoProfile = NgoProfile.builder()
                        .user(ngoUser)
                        .organizationName("Hope Foundation")
                        .domain("Education Technology")
                        .contactNumber("9876543210")
                        .preferredLanguage("en")
                        .location("Global Community")
                        .isVerified(true)
                        .build();
                ngoProfileRepository.saveAndFlush(ngoProfile);
                System.out.println("[DATABASE SEEDER] Created missing NgoProfile for ngo_test@connectingdots.org");
            }
        } catch (Exception e) {
            System.out.println("[DATABASE SEEDER] Test NGO seeding note: " + e.getMessage());
        }

        // 3. Seed / Restore Default Test Contributor User & Profile
        try {
            User contributorUser = userRepository.findByEmail("contributor_test@connectingdots.org").orElse(null);
            if (contributorUser == null) {
                contributorUser = User.builder()
                        .email("contributor_test@connectingdots.org")
                        .passwordHash(encoder.encode("password123"))
                        .role(User.Role.CONTRIBUTOR)
                        .isActive(true)
                        .build();
                contributorUser = userRepository.saveAndFlush(contributorUser);
                System.out.println("[DATABASE SEEDER] Created Test Contributor User: contributor_test@connectingdots.org / password123");
            } else {
                contributorUser.setPasswordHash(encoder.encode("password123"));
                contributorUser.setRole(User.Role.CONTRIBUTOR);
                contributorUser.setActive(true);
                contributorUser = userRepository.saveAndFlush(contributorUser);
                System.out.println("[DATABASE SEEDER] Restored Test Contributor User: contributor_test@connectingdots.org / password123");
            }

            if (!contributorProfileRepository.findByUser(contributorUser).isPresent()) {
                ContributorProfile contributorProfile = ContributorProfile.builder()
                        .user(contributorUser)
                        .firstName("Alex")
                        .lastName("Morgan")
                        .title("Senior Full-Stack Engineer")
                        .skillsSummary("React, Java, Spring Boot, Python, AI")
                        .contactNumber("9876543210")
                        .location("Community Member")
                        .preferredLanguage("en")
                        .completedProjects(2)
                        .build();
                contributorProfileRepository.saveAndFlush(contributorProfile);
                System.out.println("[DATABASE SEEDER] Created missing ContributorProfile for contributor_test@connectingdots.org");
            }
        } catch (Exception e) {
            System.out.println("[DATABASE SEEDER] Test Contributor seeding note: " + e.getMessage());
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
    
                // Ensure User exists to avoid unique constraint violations on email
                User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
                    User newUser = User.builder()
                            .email("test_webhook_ngo@example.com")
                            .passwordHash("dummy_hash")
                            .role(User.Role.NGO)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });
    
                // Ensure NgoProfile exists since it's required for ProblemStatement
                NgoProfile ngoProfile = ngoProfileRepository.findAll().stream().findFirst().orElseGet(() -> {
                    NgoProfile newNgoProfile = NgoProfile.builder()
                            .user(user)
                            .organizationName("QStash Test NGO")
                            .domain("Technology")
                            .contactNumber("1234567890")
                            .build();
                    return ngoProfileRepository.save(newNgoProfile);
                });
    
                // Create the ProblemStatement with the requested dummy data
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
    
            // Construct the IngestionMessage
            IngestionMessage message = new IngestionMessage(
                    problemStatement.getId(),
                    problemStatement.getSourceFileUrl(),
                    problemStatement.getSourceType()
            );
    
            // Dispatch the webhook
            qStashService.publishToAiService(message);
    
            System.out.println("=================================================");
            System.out.println("[AUTOMATED TEST] QStash Webhook Fired for UUID: " + problemStatement.getId());
            System.out.println("=================================================");
        }
    }
}
