package dev.tarekakel.udi.common.security;

import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * A signature check alone proves a token came from XSUAA, not that it was issued for this application.
 * Accepts tokens whose audience or client matches the bound instance.
 */
class XsuaaAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error WRONG_AUDIENCE =
            new OAuth2Error("invalid_token", "The token was not issued for this application", null);

    private final Set<String> accepted;

    XsuaaAudienceValidator(XsuaaProperties xsuaa) {
        this.accepted = Set.of(xsuaa.clientId(), xsuaa.xsappname());
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        String clientId = jwt.getClaimAsString("client_id");
        boolean audienceMatches = audience != null && audience.stream().anyMatch(accepted::contains);
        boolean clientMatches = clientId != null && accepted.contains(clientId);   // user tokens carry no client_id
        return audienceMatches || clientMatches
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(WRONG_AUDIENCE);
    }
}
