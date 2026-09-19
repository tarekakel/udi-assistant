package dev.tarekakel.udi.common.security;

/**
 * Strategy for resolving the acting user. Locally a header names the user; on BTP the XSUAA token does.
 * Callers (audit trail, JPA auditing) never know which one is active.
 */
public interface CurrentUserProvider {

    /** Identity for work that happens outside any request: seeding, scheduled jobs. */
    String SYSTEM = "system";

    String currentUser();
}
