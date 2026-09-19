package dev.tarekakel.udi.common.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Local and test profiles only: the caller names itself via the {@code X-User} header. Never active on BTP. */
@Component
@Profile("!cloud")
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER = "X-User";
    static final String ANONYMOUS = "anonymous";

    @Override
    public String currentUser() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return SYSTEM;
        }
        String user = attributes.getRequest().getHeader(HEADER);
        return (user == null || user.isBlank()) ? ANONYMOUS : user.trim();
    }
}
