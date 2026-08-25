package com.connectingdots.core_service.repository;

import com.connectingdots.core_service.entity.NgoProfile;
import com.connectingdots.core_service.entity.ProblemStatement;
import com.connectingdots.core_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class ProblemStatementRepositoryTest {

        @Autowired
        private ProblemStatementRepository problemStatementRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private NgoProfileRepository ngoProfileRepository; // Assuming you created this!

        @Test
        public void testSaveAndRetrieveProblemStatement() {
                // 1. Create and save the parent User
                User user = User.builder()
                                .email("testngo@example.com")
                                .passwordHash("hashedpass")
                                .role(User.Role.NGO)
                                .build();
                user = userRepository.save(user);

                // 2. Create and save the parent NGO Profile
                NgoProfile ngoProfile = NgoProfile.builder()
                                .user(user)
                                .organizationName("Tech for Good")
                                .domain("Education")
                                .build();
                ngoProfile = ngoProfileRepository.save(ngoProfile);

                // 3. Create and save the Problem Statement
                ProblemStatement problem = ProblemStatement.builder()
                                .ngoProfile(ngoProfile)
                                .title("Build a Student Portal")
                                .description("We need a dashboard for student metrics.")
                                .domain("Web Development")
                                .build();

                ProblemStatement savedProblem = problemStatementRepository.save(problem);

                // 4. Assertions to verify it worked in the H2 memory
                assertThat(savedProblem.getId()).isNotNull();
                assertThat(savedProblem.getStatus()).isEqualTo(ProblemStatement.Status.OPEN);
                assertThat(savedProblem.getNgoProfile().getOrganizationName()).isEqualTo("Tech for Good");
        }
}