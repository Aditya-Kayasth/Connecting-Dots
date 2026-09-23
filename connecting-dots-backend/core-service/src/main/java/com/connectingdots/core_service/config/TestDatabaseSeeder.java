package com.connectingdots.core_service.config;

import com.cloudinary.Cloudinary;
import com.connectingdots.core_service.entity.*;
import com.connectingdots.core_service.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// ==============================================================================
// Startup Seeder: Performs complete transactional cleanup and populates
// rich Maharashtra/Indian context seed data (NGOs, Problems, Contributors,
// Applications, Chat Messages, and Reviews).
// ==============================================================================
@Component
@RequiredArgsConstructor
public class TestDatabaseSeeder implements CommandLineRunner {

        private final ProblemStatementRepository problemStatementRepository;
        private final UserRepository userRepository;
        private final NgoProfileRepository ngoProfileRepository;
        private final ContributorProfileRepository contributorProfileRepository;
        private final ApplicationRepository applicationRepository;
        private final MessageRepository messageRepository;
        private final ReviewRepository reviewRepository;

        private final Optional<Cloudinary> cloudinary;

        @Override
        @Transactional
        public void run(String... args) throws Exception {
                PasswordEncoder encoder = new BCryptPasswordEncoder();

                // ----------------------------------------------------------------------
                // 0. Optional Cloudinary Media Asset Cleanup
                // ----------------------------------------------------------------------
                try {
                        if (cloudinary.isPresent()) {
                                System.out.println(
                                                "[DATABASE SEEDER] Cloudinary client present — initiating media cleanup...");
                                cloudinary.get().api().deleteResourcesByPrefix("connecting_dots/", Map.of());
                        }
                } catch (Exception cEx) {
                        System.out.println(
                                        "[DATABASE SEEDER] Cloudinary cleanup note (non-fatal): " + cEx.getMessage());
                }

                // ----------------------------------------------------------------------
                // 1. Transactional Cleanup / Reset
                // ----------------------------------------------------------------------
                System.out.println("[DATABASE SEEDER] Resetting database tables in cascading order...");
                reviewRepository.deleteAllInBatch();
                messageRepository.deleteAllInBatch();
                applicationRepository.deleteAllInBatch();
                problemStatementRepository.deleteAllInBatch();
                contributorProfileRepository.deleteAllInBatch();
                ngoProfileRepository.deleteAllInBatch();
                userRepository.deleteAllInBatch();
                System.out.println("[DATABASE SEEDER] Cleaned up existing database records successfully.");

                // ----------------------------------------------------------------------
                // 2. Seed Admin & Demo Accounts
                // ----------------------------------------------------------------------
                // Real System Admin
                User realAdmin = User.builder()
                                .email("admin@connectingdots.org")
                                .passwordHash(encoder.encode("Admin@1234"))
                                .role(User.Role.ADMIN)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(realAdmin);
                System.out.println("[DATABASE SEEDER] Seeded Real Admin: admin@connectingdots.org / Admin@1234");

                // Demo Admin Account
                User demoAdmin = User.builder()
                                .email("admin_demo@connectingdots.org")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.ADMIN)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(demoAdmin);
                System.out.println("[DATABASE SEEDER] Seeded Demo Admin: admin_demo@connectingdots.org / password123");

                // Demo NGO User & Profile
                User demoNgoUser = User.builder()
                                .email("ngo_demo@connectingdots.org")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(demoNgoUser);

                NgoProfile demoNgoProfile = NgoProfile.builder()
                                .user(demoNgoUser)
                                .organizationName("Hope Foundation")
                                .domain("Education Technology")
                                .contactNumber("9876543210")
                                .preferredLanguage("en")
                                .location("Global Community")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(demoNgoProfile);

                // Alias for legacy test ngo
                User ngoTestUser = User.builder()
                                .email("ngo_test@connectingdots.org")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngoTestUser);
                NgoProfile ngoTestProfile = NgoProfile.builder()
                                .user(ngoTestUser)
                                .organizationName("Hope Foundation Test")
                                .domain("Education Technology")
                                .contactNumber("9876543210")
                                .preferredLanguage("en")
                                .location("Global Community")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngoTestProfile);

                // Demo Contributor User & Profile
                User demoContributorUser = User.builder()
                                .email("contributor_demo@connectingdots.org")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(demoContributorUser);

                ContributorProfile demoContributorProfile = ContributorProfile.builder()
                                .user(demoContributorUser)
                                .firstName("Alex")
                                .lastName("Morgan")
                                .title("Senior Full-Stack Engineer")
                                .skillsSummary("React, Java, Spring Boot, Python, AI")
                                .contactNumber("9876543210")
                                .location("Community Member")
                                .preferredLanguage("en")
                                .completedProjects(2)
                                .build();
                contributorProfileRepository.saveAndFlush(demoContributorProfile);

                // Alias for legacy test contributor
                User contributorTestUser = User.builder()
                                .email("contributor_test@connectingdots.org")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(contributorTestUser);
                ContributorProfile contributorTestProfile = ContributorProfile.builder()
                                .user(contributorTestUser)
                                .firstName("Alex")
                                .lastName("Morgan")
                                .title("Senior Full-Stack Engineer")
                                .skillsSummary("React, Java, Spring Boot, Python, AI")
                                .contactNumber("9876543210")
                                .location("Community Member")
                                .preferredLanguage("en")
                                .completedProjects(2)
                                .build();
                contributorProfileRepository.saveAndFlush(contributorTestProfile);

                // ----------------------------------------------------------------------
                // 3. Seed 5 Maharashtra / Indian NGOs
                // ----------------------------------------------------------------------
                // NGO 1: Marathwada Jal Vikas Foundation
                User ngo1User = User.builder()
                                .email("contact@jalvikas.org")
                                .passwordHash(encoder.encode("JalVikas@2026"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngo1User);

                NgoProfile ngo1 = NgoProfile.builder()
                                .user(ngo1User)
                                .organizationName("Marathwada Jal Vikas Foundation")
                                .domain("Environment & Sustainability")
                                .contactNumber("9823456789")
                                .location("Dharashiv (Osmanabad), Maharashtra")
                                .preferredLanguage("mr")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngo1);

                // NGO 2: Netra Vikas Disability Welfare Society
                User ngo2User = User.builder()
                                .email("info@netravikas.org")
                                .passwordHash(encoder.encode("NetraVikas@2026"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngo2User);

                NgoProfile ngo2 = NgoProfile.builder()
                                .user(ngo2User)
                                .organizationName("Netra Vikas Disability Welfare Society")
                                .domain("Education Technology")
                                .contactNumber("9812345678")
                                .location("Nashik & Sambhajinagar, Maharashtra")
                                .preferredLanguage("en")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngo2);

                // NGO 3: Vidarbha Kisaan Empowerment Trust
                User ngo3User = User.builder()
                                .email("contact@vidarbhakisaan.org")
                                .passwordHash(encoder.encode("Kisaan@2026"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngo3User);

                NgoProfile ngo3 = NgoProfile.builder()
                                .user(ngo3User)
                                .organizationName("Vidarbha Kisaan Empowerment Trust")
                                .domain("Community Development")
                                .contactNumber("9730123456")
                                .location("Nagpur & Yavatmal, Maharashtra")
                                .preferredLanguage("mr")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngo3);

                // NGO 4: Baramati Gramin Shikshan Sanstha
                User ngo4User = User.builder()
                                .email("info@baramatiedu.org")
                                .passwordHash(encoder.encode("Baramati@2026"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngo4User);

                NgoProfile ngo4 = NgoProfile.builder()
                                .user(ngo4User)
                                .organizationName("Baramati Gramin Shikshan Sanstha")
                                .domain("Education Technology")
                                .contactNumber("9422012345")
                                .location("Pune & Baramati, Maharashtra")
                                .preferredLanguage("mr")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngo4);

                // NGO 5: Solapur Mahila Bachat Gat Samiti
                User ngo5User = User.builder()
                                .email("contact@solapurmahila.org")
                                .passwordHash(encoder.encode("Solapur@2026"))
                                .role(User.Role.NGO)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(ngo5User);

                NgoProfile ngo5 = NgoProfile.builder()
                                .user(ngo5User)
                                .organizationName("Solapur Mahila Bachat Gat Samiti")
                                .domain("Financial Inclusion")
                                .contactNumber("9890123456")
                                .location("Solapur, Maharashtra")
                                .preferredLanguage("mr")
                                .isVerified(true)
                                .build();
                ngoProfileRepository.saveAndFlush(ngo5);

                System.out.println("[DATABASE SEEDER] Seeded 5 Maharashtra NGOs successfully.");

                // ----------------------------------------------------------------------
                // 4. Seed 7 Technical Contributors (Maharashtra / India Context)
                // ----------------------------------------------------------------------
                User c1User = User.builder()
                                .email("aarav.deshmukh@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c1User);
                ContributorProfile c1 = ContributorProfile.builder()
                                .user(c1User)
                                .firstName("Aarav")
                                .lastName("Deshmukh")
                                .title("Senior Full-Stack & Android Engineer")
                                .skillsSummary("React, Node.js, Kotlin, Spring Boot, PostgreSQL")
                                .contactNumber("9822112233")
                                .location("Pune, Maharashtra")
                                .preferredLanguage("mr")
                                .completedProjects(4)
                                .build();
                contributorProfileRepository.saveAndFlush(c1);

                User c2User = User.builder()
                                .email("priya.kulkarni@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c2User);
                ContributorProfile c2 = ContributorProfile.builder()
                                .user(c2User)
                                .firstName("Priya")
                                .lastName("Kulkarni")
                                .title("AI & Data Science Specialist")
                                .skillsSummary("Python, PyTorch, Gemini API, Fast-API, Data Analysis")
                                .contactNumber("9833445566")
                                .location("Mumbai, Maharashtra")
                                .preferredLanguage("en")
                                .completedProjects(6)
                                .build();
                contributorProfileRepository.saveAndFlush(c2);

                User c3User = User.builder()
                                .email("rohan.patil@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c3User);
                ContributorProfile c3 = ContributorProfile.builder()
                                .user(c3User)
                                .firstName("Rohan")
                                .lastName("Patil")
                                .title("Android & Embedded Systems Developer")
                                .skillsSummary("Android, Kotlin, Offline SQLite, IVR, Twilio")
                                .contactNumber("9844556677")
                                .location("Nagpur, Maharashtra")
                                .preferredLanguage("mr")
                                .completedProjects(3)
                                .build();
                contributorProfileRepository.saveAndFlush(c3);

                User c4User = User.builder()
                                .email("ananya.joshi@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c4User);
                ContributorProfile c4 = ContributorProfile.builder()
                                .user(c4User)
                                .firstName("Ananya")
                                .lastName("Joshi")
                                .title("Accessibility & Web UX Specialist")
                                .skillsSummary("ARIA, NVDA Screen Reader, WCAG 2.1, React, CSS")
                                .contactNumber("9855667788")
                                .location("Nashik, Maharashtra")
                                .preferredLanguage("en")
                                .completedProjects(5)
                                .build();
                contributorProfileRepository.saveAndFlush(c4);

                User c5User = User.builder()
                                .email("tanmay.shinde@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c5User);
                ContributorProfile c5 = ContributorProfile.builder()
                                .user(c5User)
                                .firstName("Tanmay")
                                .lastName("Shinde")
                                .title("Cloud & DevOps Architect")
                                .skillsSummary("AWS, Docker, Microservices, Spring Cloud, Redis")
                                .contactNumber("9866778899")
                                .location("Chhatrapati Sambhajinagar, Maharashtra")
                                .preferredLanguage("mr")
                                .completedProjects(2)
                                .build();
                contributorProfileRepository.saveAndFlush(c5);

                User c6User = User.builder()
                                .email("neha.kamble@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c6User);
                ContributorProfile c6 = ContributorProfile.builder()
                                .user(c6User)
                                .firstName("Neha")
                                .lastName("Kamble")
                                .title("EdTech & Localization Developer")
                                .skillsSummary("Vue.js, Node.js, Marathi/Hindi Chatbots, WhatsApp API")
                                .contactNumber("9877889900")
                                .location("Kolhapur, Maharashtra")
                                .preferredLanguage("mr")
                                .completedProjects(3)
                                .build();
                contributorProfileRepository.saveAndFlush(c6);

                User c7User = User.builder()
                                .email("vikram.salunkhe@gmail.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(User.Role.CONTRIBUTOR)
                                .isActive(true)
                                .build();
                userRepository.saveAndFlush(c7User);
                ContributorProfile c7 = ContributorProfile.builder()
                                .user(c7User)
                                .firstName("Vikram")
                                .lastName("Salunkhe")
                                .title("IoT & Sensor Telemetry Engineer")
                                .skillsSummary("C++, Embedded C, ESP32, MQTT, Grafana, Java")
                                .contactNumber("9888990011")
                                .location("Thane, Maharashtra")
                                .preferredLanguage("en")
                                .completedProjects(4)
                                .build();
                contributorProfileRepository.saveAndFlush(c7);

                System.out.println("[DATABASE SEEDER] Seeded 7 Maharashtra Contributors successfully.");

                // ----------------------------------------------------------------------
                // 5. Seed 10 Problem Statements Across 5 NGOs
                // ----------------------------------------------------------------------
                // P1: Marathwada Handpump Alert (Marathi note context)
                ProblemStatement p1 = ProblemStatement.builder()
                                .ngoProfile(ngo1)
                                .title("Handpump Breakdown Fast-Alert System for Marathwada Villages")
                                .description("In Dharashiv (Osmanabad) district, 1,200 handpumps across 3 talukas lack rapid failure reporting. Currently, it takes 15–30 days for Gram Panchayats to inform the district water department when a handpump fails. We need a simple SMS/IVR toll-free system + web dashboard so Gram Sevaks can report breakdowns instantly and alert technicians within 24 hours.")
                                .domain("Environment & Sustainability")
                                .sourceType("IMAGE")
                                .sourceFileUrl("https://res.cloudinary.com/demo/image/upload/v1/marathi_handpump_note.jpg")
                                .status(ProblemStatement.Status.IN_PROGRESS)
                                .build();
                p1 = problemStatementRepository.saveAndFlush(p1);

                // P2: Screen Reader Accessible Job Portal (English note context)
                ProblemStatement p2 = ProblemStatement.builder()
                                .ngoProfile(ngo2)
                                .title("Screen-Reader Accessible Regional Job Portal for Visually Impaired")
                                .description("Existing job portals (Naukri, LinkedIn) are not screen-reader compatible. Over 800 registered blind and low-vision members across Nashik and Sambhajinagar cannot independently search or apply for jobs. We need an accessible web portal compliant with NVDA/JAWS screen readers supporting Marathi and English job listings.")
                                .domain("Education Technology")
                                .sourceType("IMAGE")
                                .sourceFileUrl("https://res.cloudinary.com/demo/image/upload/v1/english_accessibility_note.jpg")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p2 = problemStatementRepository.saveAndFlush(p2);

                // P3: Digital Market Access for Vidarbha Farmers
                ProblemStatement p3 = ProblemStatement.builder()
                                .ngoProfile(ngo3)
                                .title("Digital Market Access & Mandi Price Alerts for Vidarbha Farmers")
                                .description("Small cotton and soybean farmers in Yavatmal with under 2 acres are forced to sell produce at 35% below mandi rates due to lack of real-time price info. We need a WhatsApp-bot + SMS notification system delivering daily Nagpur & Amravati Mandi prices directly to farmers.")
                                .domain("Community Development")
                                .status(ProblemStatement.Status.IN_PROGRESS)
                                .build();
                p3 = problemStatementRepository.saveAndFlush(p3);

                // P4: Maternal Health Tracking in Palghar
                ProblemStatement p4 = ProblemStatement.builder()
                                .ngoProfile(ngo3)
                                .title("Offline-First Android App for ASHA Maternal Health Tracking in Palghar")
                                .description("ASHA workers in Palghar tribal blocks record pregnant women's health metrics on paper registers lost during monsoons. High-risk pregnancies are flagged too late. We need an offline-first Android application that flags high-risk pregnancies and syncs whenever connectivity is restored.")
                                .domain("Healthcare & Wellness")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p4 = problemStatementRepository.saveAndFlush(p4);

                // P5: Peer-to-Peer Learning Platform Baramati
                ProblemStatement p5 = ProblemStatement.builder()
                                .ngoProfile(ngo4)
                                .title("Peer-to-Peer Learning & Tutoring Platform for Rural Pune Students")
                                .description("First-generation school learners near Baramati drop out after Class 8 due to lack of academic support. We need a peer-tutoring matching platform where Pune college volunteers can conduct 30-min video/audio guidance sessions for rural middle schoolers.")
                                .domain("Education Technology")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p5 = problemStatementRepository.saveAndFlush(p5);

                // P6: Marathi Audio Guide for Government Schemes
                ProblemStatement p6 = ProblemStatement.builder()
                                .ngoProfile(ngo5)
                                .title("Marathi Audio & Conversational Guide for Government Welfare Schemes")
                                .description("Over 2,000 Self-Help Group (SHG) women in Solapur struggle to navigate English-only portals for schemes like Ladki Bahin and Jan Dhan. We need an interactive Marathi audio chatbot guiding users step-by-step through scheme applications.")
                                .domain("Financial Inclusion")
                                .status(ProblemStatement.Status.CLOSED)
                                .build();
                p6 = problemStatementRepository.saveAndFlush(p6);

                // P7: Solar Cold Storage Telemetry Latur
                ProblemStatement p7 = ProblemStatement.builder()
                                .ngoProfile(ngo1)
                                .title("Solar-Powered Cold Storage Inventory Telemetry in Latur")
                                .description("Community solar micro-cold storages in Latur need IoT sensor telemetry monitoring temperature and humidity levels to prevent perishable vegetable spoilage.")
                                .domain("Environment & Sustainability")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p7 = problemStatementRepository.saveAndFlush(p7);

                // P8: AI Marathi Document Summarizer Gram Panchayat
                ProblemStatement p8 = ProblemStatement.builder()
                                .ngoProfile(ngo4)
                                .title("AI-Powered Marathi Document Summarizer for Gram Panchayats")
                                .description("Gram Sevaks receive complex 40-page state GR (Government Resolutions) in PDF format daily. We need an automated AI summarizer extracting actionable key points in Marathi.")
                                .domain("Web/Software Development")
                                .status(ProblemStatement.Status.CLOSED)
                                .build();
                p8 = problemStatementRepository.saveAndFlush(p8);

                // P9: Water Quality Monitoring Chandrapur
                ProblemStatement p9 = ProblemStatement.builder()
                                .ngoProfile(ngo1)
                                .title("Clean Drinking Water Quality Sensor Monitoring in Chandrapur")
                                .description("Drinking water sources near industrial belts in Chandrapur require real-time pH and turbidity sensor logging with automated SMS alert triggers for district health officers.")
                                .domain("Environment & Sustainability")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p9 = problemStatementRepository.saveAndFlush(p9);

                // P10: Mobile Clinic Appointment & Blood Tracker Solapur
                ProblemStatement p10 = ProblemStatement.builder()
                                .ngoProfile(ngo5)
                                .title("Mobile Clinic Appointment & Blood Bank Tracker in Solapur")
                                .description("Rural clinics across Solapur district need a lightweight mobile scheduling system to coordinate mobile health van visits and track blood bag availability.")
                                .domain("Healthcare & Wellness")
                                .status(ProblemStatement.Status.OPEN)
                                .build();
                p10 = problemStatementRepository.saveAndFlush(p10);

                System.out.println("[DATABASE SEEDER] Seeded 10 Problem Statements successfully.");

                // ----------------------------------------------------------------------
                // 6. Seed Applications & Journeys
                // ----------------------------------------------------------------------
                // Application 1: Rohan Patil applying to P1 (Handpump Alert) -> ACCEPTED
                Application app1 = Application.builder()
                                .problemId(p1.getId())
                                .contributorProfileId(c3.getId())
                                .status("ACCEPTED")
                                .build();
                app1 = applicationRepository.saveAndFlush(app1);

                // Application 2: Ananya Joshi applying to P2 (Accessible Job Portal) ->
                // ACCEPTED
                Application app2 = Application.builder()
                                .problemId(p2.getId())
                                .contributorProfileId(c4.getId())
                                .status("ACCEPTED")
                                .build();
                app2 = applicationRepository.saveAndFlush(app2);

                // Application 3: Aarav Deshmukh applying to P3 (Vidarbha Farmers Mandi Alerts)
                // -> ACCEPTED
                Application app3 = Application.builder()
                                .problemId(p3.getId())
                                .contributorProfileId(c1.getId())
                                .status("ACCEPTED")
                                .build();
                app3 = applicationRepository.saveAndFlush(app3);

                // Application 4: Neha Kamble applying to P6 (Marathi Audio Guide) -> COMPLETED
                Application app4 = Application.builder()
                                .problemId(p6.getId())
                                .contributorProfileId(c6.getId())
                                .status("COMPLETED")
                                .build();
                app4 = applicationRepository.saveAndFlush(app4);

                // Application 5: Priya Kulkarni applying to P8 (AI Marathi Summarizer) ->
                // COMPLETED
                Application app5 = Application.builder()
                                .problemId(p8.getId())
                                .contributorProfileId(c2.getId())
                                .status("COMPLETED")
                                .build();
                app5 = applicationRepository.saveAndFlush(app5);

                // Application 6: Tanmay Shinde applying to P4 (Palghar ASHA app) -> PENDING
                Application app6 = Application.builder()
                                .problemId(p4.getId())
                                .contributorProfileId(c5.getId())
                                .status("PENDING")
                                .build();
                applicationRepository.saveAndFlush(app6);

                System.out.println("[DATABASE SEEDER] Seeded Applications across various statuses.");

                // ----------------------------------------------------------------------
                // 7. Seed Active Chat Messages (Journeys)
                // ----------------------------------------------------------------------
                // Chat Journey for App 1 (Handpump Alert): Rohan Patil & Marathwada Jal Vikas
                // Foundation
                Message m1 = Message.builder()
                                .applicationId(app1.getId())
                                .senderId(c3.getUser().getId())
                                .content("Namaste! I reviewed the handpump breakdown alert requirement. I can build an offline-first Android app + SMS trigger gateway using Twilio.")
                                .build();
                messageRepository.saveAndFlush(m1);

                Message m2 = Message.builder()
                                .applicationId(app1.getId())
                                .senderId(ngo1.getUser().getId())
                                .content("Dhanyawad Rohan! We have 1,200 handpumps across Dharashiv. Can the Gram Sevak submit pump numbers via IVR voice prompt as well?")
                                .build();
                messageRepository.saveAndFlush(m2);

                Message m3 = Message.builder()
                                .applicationId(app1.getId())
                                .senderId(c3.getUser().getId())
                                .content("Yes absolutely! I will configure an IVR keypress flow (1 for pump failure, 2 for status check) in Marathi.")
                                .build();
                messageRepository.saveAndFlush(m3);

                // Chat Journey for App 2 (Accessible Job Portal): Ananya Joshi & Netra Vikas
                // Society
                Message m4 = Message.builder()
                                .applicationId(app2.getId())
                                .senderId(c4.getUser().getId())
                                .content("Hello! I specialize in WCAG accessibility standards and NVDA screen reader compatibility. I would love to build this job portal for blind members in Nashik.")
                                .build();
                messageRepository.saveAndFlush(m4);

                Message m5 = Message.builder()
                                .applicationId(app2.getId())
                                .senderId(ngo2.getUser().getId())
                                .content("Welcome Ananya! That is wonderful. Our members need high-contrast screens and complete ARIA live region support in both Marathi and English.")
                                .build();
                messageRepository.saveAndFlush(m5);

                System.out.println("[DATABASE SEEDER] Seeded active chat conversations.");

                // ----------------------------------------------------------------------
                // 8. Seed Completed Project Reviews
                // ----------------------------------------------------------------------
                // Review for Neha Kamble on P6
                Review r1 = Review.builder()
                                .problemId(p6.getId())
                                .raterId(ngo5.getUser().getId())
                                .rateeId(c6.getUser().getId())
                                .rating(5)
                                .comment("Neha built an outstanding Marathi audio bot! Our SHG women in Solapur can now independently navigate welfare scheme forms.")
                                .build();
                reviewRepository.saveAndFlush(r1);

                // Review for Priya Kulkarni on P8
                Review r2 = Review.builder()
                                .problemId(p8.getId())
                                .raterId(ngo4.getUser().getId())
                                .rateeId(c2.getUser().getId())
                                .rating(5)
                                .comment("Priya's Gemini AI summarizer processes complex state GR PDFs into crisp 1-page Marathi summaries for our Gram Sevaks. Exceptional work!")
                                .build();
                reviewRepository.saveAndFlush(r2);

                System.out.println("[DATABASE SEEDER] Seeded project ratings and reviews successfully.");
                System.out.println("=================================================");
                System.out.println(
                                "[DATABASE SEEDER COMPLETE] All Maharashtra NGOs, Problems, Contributors, Applications, Chat Messages, and Reviews are active!");
                System.out.println("=================================================");
        }
}
