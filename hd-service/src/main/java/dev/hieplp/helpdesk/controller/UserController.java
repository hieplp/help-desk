package dev.hieplp.helpdesk.controller;

import dev.hieplp.helpdesk.model.dto.UserResponse;
import dev.hieplp.helpdesk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User endpoints under {@code /users}.
 */
@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Lists all users for the assign dropdown. Agent-only; the password hash
     * never leaves the server.
     *
     * @return 200 users ordered by id
     */
    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

}
