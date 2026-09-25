package dev.hieplp.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CommentTests {

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired TicketRepository tickets;

  @Autowired CommentRepository comments;

  @Autowired UserRepository users;

  @Autowired PasswordEncoder passwordEncoder;

  private long requesterId;
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

  @BeforeEach
  void seed() throws Exception {
    requesterId = users.findByEmail("requester@b.co").orElseThrow().getId();
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

  private org.springframework.test.web.servlet.ResultActions comment(
      long id, String token, String body) throws Exception {
    return mvc.perform(
        post("/tickets/" + id + "/comments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void requesterCommentsOnOwnTicket() throws Exception {
    comment(ownId, login("requester@b.co"), "{\"body\":\"Tried a different charger.\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ticketId").value(ownId))
        .andExpect(jsonPath("$.authorId").value(requesterId))
        .andExpect(jsonPath("$.body").value("Tried a different charger."));
  }

  @Test
  void agentCommentsOnAnyTicket() throws Exception {
    comment(foreignId, login("agent@b.co"), "{\"body\":\"Looking into it.\"}")
        .andExpect(status().isCreated());
  }

  @Test
  void closedTicketStillAcceptsComments() throws Exception {
    var t = tickets.findById(ownId).orElseThrow();
    t.setStatus(TicketStatus.CLOSED);
    tickets.save(t);

    comment(ownId, login("requester@b.co"), "{\"body\":\"Still broken.\"}")
        .andExpect(status().isCreated());
  }

  @Test
  void requesterForeignTicketIsSame404AsMissing() throws Exception {
    var token = login("requester@b.co");

    var foreignResult =
        comment(foreignId, token, "{\"body\":\"hi\"}")
            .andExpect(status().isNotFound())
            .andReturn();
    var missingResult =
        comment(999999, token, "{\"body\":\"hi\"}")
            .andExpect(status().isNotFound())
            .andReturn();

    assertThat(foreignResult.getResponse().getContentAsString())
        .isEqualTo(missingResult.getResponse().getContentAsString());
  }

  @Test
  void bodyValidation() throws Exception {
    var token = login("requester@b.co");

    comment(ownId, token, "{}").andExpect(status().isBadRequest());
    comment(ownId, token, "{\"body\":\"   \"}").andExpect(status().isBadRequest());
    comment(ownId, token, "{\"body\":\"" + "x".repeat(2001) + "\"}")
        .andExpect(status().isBadRequest());
    comment(ownId, token, "{\"body\":\"ok\",\"extra\":1}").andExpect(status().isBadRequest());
  }

  @Test
  void newCommentAppearsLastInDetail() throws Exception {
    var token = login("requester@b.co");
    comment(ownId, token, "{\"body\":\"first\"}").andExpect(status().isCreated());
    comment(ownId, token, "{\"body\":\"second\"}").andExpect(status().isCreated());

    var result =
        mvc.perform(get("/tickets/" + ownId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    var list = objectMapper.readTree(result.getResponse().getContentAsString()).get("comments");
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(1).get("body").asString()).isEqualTo("second");
  }
}
