package dev.tarekakel.udi.common.security;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Boots the cloud profile with a stubbed JwtDecoder: signature verification is XSUAA's job,
 * what we own is scope enforcement and who ends up in the audit trail.
 */
@SpringBootTest(properties = {
        "spring.profiles.active=cloud",
        "app.seed-data=false",
        "app.security.xsuaa.url=https://xsuaa.invalid",
        "app.security.xsuaa.xsappname=" + CloudSecurityTest.XSAPPNAME,
        "app.security.xsuaa.client-id=sb-" + CloudSecurityTest.XSAPPNAME
})
class CloudSecurityTest {

    static final String XSAPPNAME = "udi-assistant!t1";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy securityFilterChain;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
        when(jwtDecoder.decode("viewer-token")).thenReturn(token("viewer@example.com", Scopes.VIEWER));
        when(jwtDecoder.decode("editor-token")).thenReturn(token("editor@example.com", Scopes.VIEWER, Scopes.EDITOR));
        when(jwtDecoder.decode("bad-token")).thenThrow(new BadJwtException("signature"));   // what Nimbus throws
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void rejectsMissingAndInvalidTokens() throws Exception {
        mvc.perform(get("/api/devices")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/devices").header("Authorization", "Bearer bad-token")).andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/devices").header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/devices").header("Authorization", "Bearer viewer-token")
                        .contentType(APPLICATION_JSON).content(createBody("04000000000006")))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorWritesAndTheTokenUserLandsInTheAuditTrail() throws Exception {
        String location = mvc.perform(post("/api/devices").header("Authorization", "Bearer editor-token")
                        .contentType(APPLICATION_JSON).content(createBody("04012345678901")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("editor@example.com"))
                .andReturn().getResponse().getHeader("Location");
        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(get("/api/devices/{id}/audit-trail", id).header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATE"))
                .andExpect(jsonPath("$[0].performedBy").value("editor@example.com"));
    }

    private static Jwt token(String user, String... localScopes) {
        List<String> scopes = Arrays.stream(localScopes).map(scope -> XSAPPNAME + "." + scope).toList();
        return Jwt.withTokenValue(user).header("alg", "RS256").subject(user)
                .claim("user_name", user).claim("scope", scopes).audience(List.of(XSAPPNAME))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
    }

    private static String createBody(String udiDi) {
        return """
                {"udiDi":"%s","name":"Secured Device","manufacturer":"Test Manufacturer GmbH","riskClass":"IIA"}
                """.formatted(udiDi);
    }
}
