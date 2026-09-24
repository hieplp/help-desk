package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> listForCaller(Long callerId);

}
