package dev.hieplp.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateTicketTests {

    private static final String VALID_BODY = """
            {"title":"Laptop will not boot","description":"Black screen after the logo.",
             "category":"hardware","priority":"high"}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private String login(String email) throws Exception {
        var result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").get("value").asString();
    }

    private long callerId(String token) {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload)).get("sub").asLong();
    }

    @Test
    void validBodyCreatesOpenTicket() throws Exception {
        String token = login("requester@b.co");
        var result = mvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.propertyNames()).isEqualTo(Set.of(
                "id", "title", "description", "category", "priority",
                "status", "requesterId", "assigneeId", "createdAt", "updatedAt"));
        assertThat(body.get("title").asString()).isEqualTo("Laptop will not boot");
        assertThat(body.get("description").asString()).isEqualTo("Black screen after the logo.");
        assertThat(body.get("category").asString()).isEqualTo("hardware");
        assertThat(body.get("priority").asString()).isEqualTo("high");
        assertThat(body.get("status").asString()).isEqualTo("open");
        assertThat(body.get("requesterId").asLong()).isEqualTo(callerId(token));
        assertThat(body.get("assigneeId").isNull()).isTrue();
        assertThat(body.get("createdAt").asString()).isEqualTo(body.get("updatedAt").asString());
    }

    @Test
    void trimsTitleAndDescription() throws Exception {
        mvc.perform(post("/tickets")
                        .header("Authorization", "Bearer " + login("requester@b.co"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  hi  \",\"description\":\"  x  \",\"category\":\"other\",\"priority\":\"low\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("hi"))
                .andExpect(jsonPath("$.description").value("x"));
    }

    @Test
    void badInputIs400() throws Exception {
        String token = login("requester@b.co");
        String[] bodies = {
                // missing / blank / over-limit title
                "{\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\"}",
                "{\"title\":\"   \",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\"}",
                "{\"title\":\"" + "t".repeat(121) + "\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\"}",
                // missing / blank / over-limit description
                "{\"title\":\"t\",\"category\":\"hardware\",\"priority\":\"high\"}",
                "{\"title\":\"t\",\"description\":\"  \",\"category\":\"hardware\",\"priority\":\"high\"}",
                "{\"title\":\"t\",\"description\":\"" + "d".repeat(4001) + "\",\"category\":\"hardware\",\"priority\":\"high\"}",
                // wrong-case / unknown / missing enums
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"Hardware\",\"priority\":\"high\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"network\",\"priority\":\"high\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"priority\":\"high\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"HIGH\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"urgent\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\"}",
                // caller-controlled fields
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\",\"status\":\"closed\"}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\",\"requesterId\":1}",
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\",\"assigneeId\":1}",
                // unknown field
                "{\"title\":\"t\",\"description\":\"d\",\"category\":\"hardware\",\"priority\":\"high\",\"foo\":1}",
        };
        for (String body : bodies) {
            mvc.perform(post("/tickets")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("bad_request"));
        }
    }

    @Test
    void noOrBadTokenGets401() throws Exception {
        mvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(post("/tickets")
                        .header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }
}
