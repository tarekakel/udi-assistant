package dev.tarekakel.udi.device;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.tarekakel.udi.common.security.HeaderCurrentUserProvider;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "app.seed-data=false")
class DeviceApiTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsADeviceInDraft() throws Exception {
        String udiDi = uniqueUdiDi();

        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content(createBody(udiDi)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.udiDi").value(udiDi))
                .andExpect(jsonPath("$.registrationStatus").value("DRAFT"))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("SUBMITTED"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdBy").value("anonymous"));
    }

    @Test
    void rejectsDuplicateUdiDiWithProblemDetail() throws Exception {
        String udiDi = uniqueUdiDi();
        create(udiDi);

        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content(createBody(udiDi)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate UDI-DI"))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void rejectsInvalidCheckDigitNamingTheField() throws Exception {
        mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content(createBody("04012345678902")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("udiDi"));
    }

    @Test
    void recordsWhoChangedWhatAndWhy() throws Exception {
        String id = create(uniqueUdiDi());

        mvc.perform(put("/api/devices/{id}", id).contentType(APPLICATION_JSON)
                        .header(HeaderCurrentUserProvider.HEADER, "tarek")
                        .content("""
                                {"name":"Renamed Device","manufacturer":"Test Manufacturer GmbH","riskClass":"IIA",
                                 "version":0,"reason":"Label correction after audit finding"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.updatedBy").value("tarek"));

        mvc.perform(get("/api/devices/{id}/audit-trail", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CREATE"))
                .andExpect(jsonPath("$[0].performedBy").value("anonymous"))
                .andExpect(jsonPath("$[1].action").value("UPDATE"))
                .andExpect(jsonPath("$[1].field").value("name"))
                .andExpect(jsonPath("$[1].oldValue").value("Test Device"))
                .andExpect(jsonPath("$[1].newValue").value("Renamed Device"))
                .andExpect(jsonPath("$[1].reason").value("Label correction after audit finding"))
                .andExpect(jsonPath("$[1].performedBy").value("tarek"));
    }

    @Test
    void recentTrailSpansAllDevicesNewestFirst() throws Exception {
        String first = create(uniqueUdiDi());
        String second = create(uniqueUdiDi());

        mvc.perform(get("/api/audit-trail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityType").value("Device"))
                .andExpect(jsonPath("$[0].entityId").value(second))
                .andExpect(jsonPath("$[1].entityId").value(first));
    }

    @Test
    void rejectsStaleVersion() throws Exception {
        String id = create(uniqueUdiDi());

        mvc.perform(put("/api/devices/{id}", id).contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"X","manufacturer":"Y","riskClass":"I","version":7,"reason":"stale"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Stale version"));
    }

    @Test
    void enforcesTheStatusLifecycle() throws Exception {
        String id = create(uniqueUdiDi());

        mvc.perform(post("/api/devices/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"REGISTERED","reason":"skip the queue"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid status transition"));

        mvc.perform(post("/api/devices/{id}/status", id).contentType(APPLICATION_JSON)
                        .content("""
                                {"status":"SUBMITTED","reason":"Technical file complete"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.allowedTransitions.length()").value(2));
    }

    @Test
    void returnsProblemDetailForUnknownDevice() throws Exception {
        mvc.perform(get("/api/devices/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Device not found"));
    }

    private String create(String udiDi) throws Exception {
        String location = mvc.perform(post("/api/devices").contentType(APPLICATION_JSON).content(createBody(udiDi)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static String createBody(String udiDi) {
        return """
                {"udiDi":"%s","name":"Test Device","manufacturer":"Test Manufacturer GmbH","riskClass":"IIA"}
                """.formatted(udiDi);
    }

    /** Distinct valid GTIN-14 per call: payload 0400000000 + 3-digit sequence, check digit computed. */
    private static String uniqueUdiDi() {
        String payload = "0400000000" + String.format("%03d", SEQUENCE.incrementAndGet());
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.digit(payload.charAt(i), 10) * ((13 - i) % 2 == 1 ? 3 : 1);
        }
        return payload + (10 - sum % 10) % 10;
    }
}
