package dev.hieplp.helpdesk.model.dto.user;

import dev.hieplp.helpdesk.model.entity.User;
import dev.hieplp.helpdesk.model.enums.Role;

/**
 * Public user profile. The password hash is never included.
 *
 * @param id user id
 * @param name display name
 * @param email account email
 * @param role {@code requester} or {@code agent}
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role
) {

    /**
     * Maps a {@link User} entity to its public shape.
     *
     * @param user entity
     * @return id, name, email, role
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

}
