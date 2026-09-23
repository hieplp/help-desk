package dev.hieplp.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private MvcResult login(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        for (String email : new String[]{"a@b.co", "A@B.co"}) {
            MvcResult result = login(email, "secret");
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

            String token = body.get("token").get("value").asString();
            assertThat(token).isNotBlank();
            assertThat(body.get("token").get("expiresAt").asString())
                    .isEqualTo(Instant.ofEpochSecond(claims(token).get("exp").asLong()).toString());
            assertThat(body.get("user").get("email").asString()).isEqualTo("a@b.co");
            assertThat(body.get("user").get("role").asString()).isEqualTo("agent");
            assertThat(body.toString()).doesNotContain("passwordHash");
        }
    }

    @Test
    void badCredentialsAreOne401() throws Exception {
        for (String[] creds : new String[][]{{"a@b.co", "wrong"}, {"nobody@b.co", "secret"}, {" nobody@b.co ", "secret"}}) {
            MvcResult result = login(creds[0], creds[1]);
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(body.get("code").asString()).isEqualTo("unauthorized");
            assertThat(body.get("message").asString()).isEqualTo("Invalid email or password");
        }
    }

    @Test
    void badInputIs400() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  \",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.co\",\"password\":\"secret\",\"x\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.co\",\"password\":\"" + "x".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"email\":\"a@b.co\",\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("bad_request"))
                .andExpect(jsonPath("$.message").value("Request is invalid"));
    }

    @Test
    void otherRoutesRequireAToken() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
        mvc.perform(get("/users").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        MvcResult result = login("a@b.co", "secret");
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").get("value").asString();
        MvcResult protectedResult = mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andReturn();
        assertThat(protectedResult.getResponse().getStatus()).isNotEqualTo(401);
    }

    private JsonNode claims(String token) {
        String payload = token.split("\\.")[1];
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload));
    }
}
