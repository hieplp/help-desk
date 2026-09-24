package dev.hieplp.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GetUsersTests {

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

    @Test
    void agentGetsAllUsers() throws Exception {
        var result = mvc.perform(get("/users")
                        .header("Authorization", "Bearer " + login("agent@b.co")))
                .andExpect(status().isOk())
                .andReturn();

        String raw = result.getResponse().getContentAsString();
        assertThat(raw).doesNotContain("password");

        JsonNode users = objectMapper.readTree(raw);
        assertThat(users.isArray()).isTrue();
        assertThat(users.size()).isGreaterThanOrEqualTo(2);

        var emails = new HashSet<String>();
        long previousId = 0;
        for (JsonNode user : users) {
            assertThat(user.isObject()).isTrue();
            assertThat(user.propertyNames()).isEqualTo(Set.of("id", "name", "email", "role"));
            assertThat(user.get("id").asLong()).isGreaterThan(previousId);
            assertThat(user.get("name").isString()).isTrue();
            assertThat(user.get("email").isString()).isTrue();
            assertThat(user.get("role").asString()).isIn("requester", "agent");
            previousId = user.get("id").asLong();
            emails.add(user.get("email").asString());
        }
        assertThat(emails).contains("agent@b.co", "requester@b.co");
    }

    @Test
    void requesterGets403() throws Exception {
        mvc.perform(get("/users")
                        .header("Authorization", "Bearer " + login("requester@b.co")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void noOrBadTokenGets401() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
        mvc.perform(get("/users").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"));
    }
}
