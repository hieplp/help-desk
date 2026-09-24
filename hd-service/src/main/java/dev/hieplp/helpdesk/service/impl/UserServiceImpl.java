package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.exception.ApiException;
import dev.hieplp.helpdesk.model.dto.UserResponse;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserResponse> listForCaller(Long callerId) {
        var caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Forbidden"));
        if (caller.getRole() != Role.AGENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(UserResponse::from)
                .toList();
    }
}
