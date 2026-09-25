package dev.hieplp.helpdesk;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.hieplp.helpdesk.security.principal.Caller;
import dev.hieplp.helpdesk.security.principal.CurrentCaller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CurrentCallerTests.ProbeController.class)
class CurrentCallerTests {

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @RestController
  static class ProbeController {
    @GetMapping("/probe/me")
    Long me(@CurrentCaller Caller caller) {
      return caller.id();
    }
  }

  @Test
  void currentCallerResolvesUserId() throws Exception {
    MvcResult login =
        mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"agent@b.co\",\"password\":\"secret\"}"))
            .andReturn();
    JsonNode body = objectMapper.readTree(login.getResponse().getContentAsString());
    String token = body.get("token").get("value").asString();
    long userId = body.get("user").get("id").asLong();

    mvc.perform(get("/probe/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(userId));
  }

  @Test
  void missingOrBadTokenIs401() throws Exception {
    mvc.perform(get("/probe/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("unauthorized"));
    mvc.perform(get("/probe/me").header("Authorization", "Bearer garbage"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("unauthorized"));
  }
}
