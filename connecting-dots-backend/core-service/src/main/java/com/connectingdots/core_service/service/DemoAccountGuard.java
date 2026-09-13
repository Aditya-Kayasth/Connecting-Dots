package com.connectingdots.core_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Shared guard that enforces read-only behaviour for all seeded demo accounts.
 *
 * Inject this bean into any service that handles write operations and call
 * {@link #assertNotDemoAccount()} at the top of every mutating method.
 *
 * Demo accounts:
 *   admin@connectingdots.org        — shared test admin   (read-only admin panel)
 *   ngo_test@connectingdots.org     — shared test NGO     (read-only NGO workspace)
 *   contributor_test@connectingdots.org — shared test contributor (read-only contributor view)
 */
@Component
public class DemoAccountGuard {

    public static final Set<String> DEMO_EMAILS = Set.of(
            "admin@connectingdots.org",
            "ngo_test@connectingdots.org",
            "contributor_test@connectingdots.org",
            "demo.ngo@connectingdots.org",
            "demo.contributor@connectingdots.org",
            "demo.admin@connectingdots.org",
            "demo_ngo@connectingdots.org",
            "demo_contributor@connectingdots.org",
            "demo_admin@connectingdots.org"
    );

    /**
     * Throws 403 FORBIDDEN if the currently authenticated user is a demo account.
     * Safe to call from any thread that has an active Spring Security context.
     */
    public void assertNotDemoAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email != null && DEMO_EMAILS.contains(email.trim().toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This shared demo account is read-only. " +
                    "Create your own free account to submit problems, apply, or send messages."
            );
        }
    }
}
