package dev.hieplp.helpdesk.repository;

import dev.hieplp.helpdesk.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * User persistence.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by exact email (emails are stored lowercased).
     *
     * @param email normalized email
     * @return the user, or empty
     */
    Optional<User> findByEmail(String email);

    /**
     * All users ordered by id — the assign-dropdown list.
     *
     * @return every user
     */
    List<User> findAllByOrderByIdAsc();

}
