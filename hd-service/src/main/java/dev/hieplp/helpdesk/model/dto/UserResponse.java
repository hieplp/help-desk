package dev.hieplp.helpdesk.model.dto;

import dev.hieplp.helpdesk.model.entity.User;
import dev.hieplp.helpdesk.model.enums.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

}
