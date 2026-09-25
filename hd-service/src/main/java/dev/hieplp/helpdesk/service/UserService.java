package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.UserResponse;

import java.util.List;

/**
 * User lookups.
 */
public interface UserService {
    /**
     * Lists every user, ordered by id.
     *
     * @return all users; password hash is never exposed
     */
    List<UserResponse> list();

}
