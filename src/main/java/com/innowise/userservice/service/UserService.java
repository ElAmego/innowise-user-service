package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserDao userDao;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserDao userDao, UserMapper userMapper) {
        this.userDao = userDao;
        this.userMapper = userMapper;
    }

    public UserDto createUser(UserDto userDto) {
        if (userDto == null) {
            return null;
        }

        final User user = userMapper.toEntity(userDto);
        final String userEmail = user.getEmail();
        final boolean isExist = userDao.existsByEmail(userEmail);

        if (!isExist) {
            user.setActive(true);
            userDao.saveUser(user);

            return userMapper.toDto(user);
        } else {
            return null;
        }
    }

    public UserDto getUserById(Long id) {
        if (id == null) {
            return null;
        }

        final Optional<User> userOptional = userDao.findUserById(id);

        if (userOptional.isPresent()) {
            final User user = userOptional.get();

            return userMapper.toDto(user);
        }

        return null;
    }

    public Page<UserDto> getAllUsers(Pageable pageable) {
        if (pageable == null) {
            return null;
        }

        return userDao.findAll(pageable)
                .map(userMapper::toDto);
    }

    public UserDto updateUser(UserDto userDto) {
        if (userDto == null) {
            return null;
        }

        final Long userDtoId = userDto.getId();
        final Optional<User> userOptional = userDao.findUserById(userDtoId);

        if (userOptional.isPresent()) {
            final User user = userOptional.get();
            userDao.updateUserById(user);

            return userMapper.toDto(user);
        }

        return null;
    }

    public boolean activateUser(Long id) {
        if (id == null) {
            return false;
        }

        final int updated = userDao.activateUserById(id);

        return updated != 0;
    }

    public boolean deactivateUser(Long id) {
        if (id == null) {
            return false;
        }

        final int updated = userDao.deactivateUserById(id);

        return updated != 0;
    }
}