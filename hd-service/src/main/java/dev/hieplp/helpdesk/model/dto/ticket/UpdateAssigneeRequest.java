package dev.hieplp.helpdesk.model.dto.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;

/**
 * {@code PATCH /tickets/:id/assignee} body — requires the {@code assigneeId} key. The setter marks
 * the key as provided so an absent {@code assigneeId} (→ 400) stays distinct from
 * {@code assigneeId: null} (→ unassign). Type-mismatched values fail binding → 400.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateAssigneeRequest {

  private Long assigneeId;

  @JsonIgnore private boolean provided;

  /** Binds only when the key is present in the JSON body. */
  @JsonSetter("assigneeId")
  public void setAssigneeId(Long assigneeId) {
    this.assigneeId = assigneeId;
    this.provided = true;
  }
}
