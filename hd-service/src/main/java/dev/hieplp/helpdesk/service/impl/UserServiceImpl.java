package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.model.dto.user.UserResponse;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Straight {@link UserRepository} reads mapped to {@link UserResponse}. */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  /** {@inheritDoc} */
  @Override
  public List<UserResponse> list() {
    return userRepository.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
  }
}
