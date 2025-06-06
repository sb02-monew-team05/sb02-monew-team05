package com.part2.monew.service.impl;

import com.part2.monew.dto.request.UserCreateRequest;
import com.part2.monew.dto.request.UserLoginRequest;
import com.part2.monew.dto.request.UserUpdateRequest;
import com.part2.monew.dto.response.UserResponse;
import com.part2.monew.entity.User;
import com.part2.monew.global.exception.user.EmailDuplicateException;
import com.part2.monew.global.exception.user.NoPermissionToDeleteException;
import com.part2.monew.global.exception.user.NoPermissionToUpdateException;
import com.part2.monew.global.exception.user.UserNotFoundException;
import com.part2.monew.mapper.UserMapper;
import com.part2.monew.repository.UserRepository;
import com.part2.monew.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserResponse createUser(UserCreateRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new EmailDuplicateException();
        }

        User user = userMapper.toEntity(request);
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    @Override
    public User loginUser(UserLoginRequest request){
        String email = request.email();
        String password = request.password();

        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(!user.getPassword().equals(password)){
            throw new RuntimeException("Incorrect password");
        }
        return user;
    }

    @Transactional
    @Override
    public UserResponse updateNickname(UUID userId, UUID requestUserId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToUpdateException();
        }

        user.setNickname(request.getNickname());
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    @Override
    public void delete(UUID userId, UUID requestUserId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToDeleteException();
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void deleteHard(UUID userId, UUID requestUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getId().equals(requestUserId)){
            throw new NoPermissionToDeleteException();
        }

        userRepository.delete(user);
    }
}