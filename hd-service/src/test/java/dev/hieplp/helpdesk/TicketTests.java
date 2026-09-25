package dev.hieplp.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.hieplp.helpdesk.model.entity.Comment;
import dev.hieplp.helpdesk.model.entity.User;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.repository.CommentRepository;
import dev.hieplp.helpdesk.repository.TicketRepository;
import dev.hieplp.helpdesk.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class TicketTests {

  private static final Set<String> LIST_ITEM_KEYS =
      Set.of(
          "id",
          "title",
          "category",
          "priority",
          "status",
          "requesterId",
          "requesterName",
          "assigneeId",
          "createdAt",
          "updatedAt");

  @Autowired MockMvc mvc;

  @Autowired ObjectMapper objectMapper;

  @Autowired TicketRepository tickets;

  @Autowired CommentRepository comments;

  @Autowired UserRepository users;

  @Autowired PasswordEncoder passwordEncoder;

  private long requesterId;
  private long agentId;
  private long ownOpenId;
  private long ownInProgressId;
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
    agentId = users.findByEmail("agent@b.co").orElseThrow().getId();
    var other =
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

    var requesterToken = login("requester@b.co");
    var otherToken = login("requester2@b.co");

    // created in order; updatedAt ties are broken by id desc in assertions below
    ownOpenId = createTicket(requesterToken, "Own open");
    ownInProgressId = createTicket(requesterToken, "Own in progress");
    foreignId = createTicket(otherToken, "Foreign");

    var inProgress = tickets.findById(ownInProgressId).orElseThrow();
    inProgress.setStatus(dev.hieplp.helpdesk.model.enums.TicketStatus.IN_PROGRESS);
    tickets.save(inProgress);

    var own = tickets.findById(ownOpenId).orElseThrow();
    var c1 = new Comment();
    c1.setTicket(own);
    c1.setAuthor(users.getReferenceById(requesterId));
    c1.setBody("first");
    comments.save(c1);
    var c2 = new Comment();
    c2.setTicket(own);
    c2.setAuthor(users.getReferenceById(agentId));
    c2.setBody("second");
    comments.save(c2);
  }

  @Test
  void requesterListsOwnTicketsOnly() throws Exception {
    var result =
        mvc.perform(get("/tickets").header("Authorization", "Bearer " + login("requester@b.co")))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(list.isArray()).isTrue();
    assertThat(list.size()).isEqualTo(2);
    for (JsonNode item : list) {
      assertThat(item.propertyNames()).isEqualTo(LIST_ITEM_KEYS);
      assertThat(item.get("requesterId").asLong()).isEqualTo(requesterId);
    }
    // newest updatedAt first: the IN_PROGRESS ticket was updated last
    assertThat(list.get(0).get("id").asLong()).isEqualTo(ownInProgressId);
  }

  @Test
  void agentListsAllTickets() throws Exception {
    var result =
        mvc.perform(get("/tickets").header("Authorization", "Bearer " + login("agent@b.co")))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.get(0).get("id").asLong()).isEqualTo(ownInProgressId);
  }

  @Test
  void statusFilter() throws Exception {
    var token = login("requester@b.co");

    var result =
        mvc.perform(get("/tickets?status=open").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode list = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0).get("id").asLong()).isEqualTo(ownOpenId);

    mvc.perform(get("/tickets?status=bogus").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("bad_request"));
  }

  @Test
  void detailHasDescriptionAndCommentsOldestFirst() throws Exception {
    var result =
        mvc.perform(
                get("/tickets/" + ownOpenId)
                    .header("Authorization", "Bearer " + login("requester@b.co")))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode detail = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(detail.get("description").asString()).isEqualTo("Details");
    JsonNode list = detail.get("comments");
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).get("body").asString()).isEqualTo("first");
    assertThat(list.get(1).get("body").asString()).isEqualTo("second");
    assertThat(list.get(0).get("id").asLong()).isLessThan(list.get(1).get("id").asLong());
  }

  @Test
  void requesterForeignTicketIsSame404AsMissing() throws Exception {
    var token = login("requester@b.co");

    var foreignResult =
        mvc.perform(get("/tickets/" + foreignId).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andReturn();
    var missingResult =
        mvc.perform(get("/tickets/999999").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andReturn();

    assertThat(foreignResult.getResponse().getContentAsString())
        .isEqualTo(missingResult.getResponse().getContentAsString());
  }

  @Test
  void agentGetsAnyTicket() throws Exception {
    mvc.perform(
            get("/tickets/" + foreignId).header("Authorization", "Bearer " + login("agent@b.co")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(foreignId));
  }

  @Test
  void badIdsGet400() throws Exception {
    var token = login("agent@b.co");
    mvc.perform(get("/tickets/abc").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/tickets/-1").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void noOrBadTokenGets401() throws Exception {
    mvc.perform(get("/tickets")).andExpect(status().isUnauthorized());
    mvc.perform(get("/tickets/1").header("Authorization", "Bearer garbage"))
        .andExpect(status().isUnauthorized());
  }
}
