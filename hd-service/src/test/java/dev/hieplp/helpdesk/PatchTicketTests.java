package dev.hieplp.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.hieplp.helpdesk.model.entity.User;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.model.enums.TicketStatus;
import dev.hieplp.helpdesk.repository.CommentRepository;
import dev.hieplp.helpdesk.repository.TicketRepository;
import dev.hieplp.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PatchTicketTests {

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired TicketRepository tickets;

  @Autowired CommentRepository comments;

  @Autowired UserRepository users;

  @Autowired PasswordEncoder passwordEncoder;

  private long requesterId;
  private long agentId;
  private long ownId;
  private long foreignId;

  private String login(String email) throws Exception {
    var result =
        mvc.perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"secret\"}"))
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("token")
        .get("value")
        .asString();
  }

  private long createTicket(String token, String title) throws Exception {
    var result =
        mvc.perform(
                post("/tickets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\""
                            + title
                            + "\",\"description\":\"Details\","
                            + "\"category\":\"software\",\"priority\":\"medium\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private void setStatus(long ticketId, TicketStatus status) {
    var t = tickets.findById(ticketId).orElseThrow();
    t.setStatus(status);
    tickets.save(t);
  }

  @BeforeEach
  void seed() throws Exception {
    requesterId = users.findByEmail("requester@b.co").orElseThrow().getId();
    agentId = users.findByEmail("agent@b.co").orElseThrow().getId();
    users
        .findByEmail("requester2@b.co")
        .orElseGet(
            () -> {
              var u = new User();
              u.setEmail("requester2@b.co");
              u.setName("Requester Two");
              u.setRole(Role.REQUESTER);
              u.setPasswordHash(passwordEncoder.encode("secret"));
              return users.save(u);
            });

    comments.deleteAll();
    tickets.deleteAll();

    ownId = createTicket(login("requester@b.co"), "Own");
    foreignId = createTicket(login("requester2@b.co"), "Foreign");
  }

  private org.springframework.test.web.servlet.ResultActions patchTicket(
      long id, String token, String body) throws Exception {
    return mvc.perform(
        patch("/tickets/" + id)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private org.springframework.test.web.servlet.ResultActions patchAssignee(
      long id, String token, String body) throws Exception {
    return mvc.perform(
        patch("/tickets/" + id + "/assignee")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void agentMovesThroughLifecycle() throws Exception {
    var token = login("agent@b.co");

    patchTicket(ownId, token, "{\"status\":\"in_progress\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("in_progress"));
    patchTicket(ownId, token, "{\"status\":\"resolved\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("resolved"));
    patchTicket(ownId, token, "{\"status\":\"closed\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("closed"));
  }

  @Test
  void agentCannotSetOpen() throws Exception {
    patchTicket(ownId, login("agent@b.co"), "{\"status\":\"open\"}")
        .andExpect(status().isForbidden());
  }

  @Test
  void requesterClosesOwnTicketOnly() throws Exception {
    var token = login("requester@b.co");

    patchTicket(ownId, token, "{\"status\":\"closed\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("closed"));

    var other = createTicket(token, "Other");
    patchTicket(other, token, "{\"status\":\"in_progress\"}")
        .andExpect(status().isForbidden());
    patchTicket(other, token, "{\"status\":\"resolved\"}").andExpect(status().isForbidden());
    patchTicket(other, token, "{\"status\":\"open\"}").andExpect(status().isForbidden());
  }

  @Test
  void requesterForeignTicketIs404() throws Exception {
    patchTicket(foreignId, login("requester@b.co"), "{\"status\":\"closed\"}")
        .andExpect(status().isNotFound());
  }

  @Test
  void closedTicketRejectsAnyPatch() throws Exception {
    setStatus(ownId, TicketStatus.CLOSED);
    var agent = login("agent@b.co");
    var requester = login("requester@b.co");

    patchTicket(ownId, agent, "{\"status\":\"resolved\"}").andExpect(status().isForbidden());
    patchAssignee(ownId, agent, "{\"assigneeId\":" + agentId + "}")
        .andExpect(status().isForbidden());
    patchTicket(ownId, requester, "{\"status\":\"closed\"}").andExpect(status().isForbidden());
  }

  @Test
  void assigneeRules() throws Exception {
    var token = login("agent@b.co");

    patchAssignee(ownId, token, "{\"assigneeId\":" + agentId + "}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assigneeId").value(agentId))
        .andExpect(jsonPath("$.assigneeName").value("Agent"));

    patchAssignee(ownId, token, "{\"assigneeId\":null}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assigneeId").doesNotExist());

    patchAssignee(ownId, token, "{\"assigneeId\":" + requesterId + "}")
        .andExpect(status().isBadRequest());
    patchAssignee(ownId, token, "{\"assigneeId\":999999}").andExpect(status().isBadRequest());
    patchAssignee(ownId, token, "{\"assigneeId\":\"x\"}").andExpect(status().isBadRequest());
    patchAssignee(ownId, token, "{}").andExpect(status().isBadRequest());
  }

  @Test
  void requesterCannotAssign() throws Exception {
    patchAssignee(ownId, login("requester@b.co"), "{\"assigneeId\":" + agentId + "}")
        .andExpect(status().isForbidden());
    // status endpoint does not carry assigneeId — unknown field → 400
    patchTicket(ownId, login("requester@b.co"), "{\"assigneeId\":" + agentId + "}")
        .andExpect(status().isBadRequest());
  }

  @Test
  void fieldErrorsBeatRoleErrors() throws Exception {
    var token = login("requester@b.co");

    patchTicket(ownId, token, "{}").andExpect(status().isBadRequest());
    patchTicket(ownId, token, "{\"title\":\"x\"}").andExpect(status().isBadRequest());
    patchTicket(ownId, token, "{\"status\":\"bogus\"}").andExpect(status().isBadRequest());
    patchTicket(ownId, token, "{\"status\":\"OPEN\"}").andExpect(status().isBadRequest());
    patchTicket(ownId, token, "{\"status\":null}").andExpect(status().isBadRequest());
  }

  @Test
  void patchResponseHasNoComments() throws Exception {
    var result =
        patchTicket(ownId, login("agent@b.co"), "{\"status\":\"in_progress\"}")
            .andExpect(status().isOk())
            .andReturn();
    var node = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(node.has("comments")).isFalse();
    assertThat(node.has("description")).isTrue();
  }

  @Test
  void badIdsAndMissingTicket() throws Exception {
    var token = login("agent@b.co");
    mvc.perform(
            patch("/tickets/-1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"closed\"}"))
        .andExpect(status().isBadRequest());
    patchTicket(999999, token, "{\"status\":\"closed\"}").andExpect(status().isNotFound());
  }
}
