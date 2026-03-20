package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.dto.UserWithCardsDto;
import com.innowise.userservice.exception.custom.DuplicateEmailException;
import com.innowise.userservice.exception.custom.InvalidUserDataException;
import com.innowise.userservice.exception.custom.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.model.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final PaymentCardDao paymentCardDao;
    private final UserMapper userMapper;
    private final PaymentCardMapper paymentCardMapper;

    @CacheEvict(value = "userWithCards", allEntries = true)
    public boolean createUser(UserDto userDto) {
        if (userDto == null) {
            throw new InvalidUserDataException("User data cannot be null");
        }

        final User user = userMapper.toEntity(userDto);
        final String userEmail = user.getEmail();
        final boolean isExist = userDao.existsByEmail(userEmail);

        if (!isExist) {
            user.setActive(true);

            return userDao.saveUser(user) > 0;
        } else {
            throw new DuplicateEmailException(userDto.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAll(String name, String surname, Pageable pageable) {
        final Specification<User> spec = UserSpecification.byNameAndSurname(name, surname);

        return userDao.findAll(spec, pageable)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        if (id == null) {
            throw new InvalidUserDataException("ID cannot be null");
        }

        final Optional<User> userOptional = userDao.findUserById(id);

        if (userOptional.isPresent()) {
            final User user = userOptional.get();

            return userMapper.toDto(user);
        } else {
            throw new UserNotFoundException(id);
        }
    }

    @Cacheable(value = "userWithCards", key = "#id")
    @Transactional(readOnly = true)
    public UserWithCardsDto getUserWithCardsById(Long id) {
        if (id == null) {
            throw new InvalidUserDataException("ID cannot be null");
        }

        final Optional<User> userOptional = userDao.findUserById(id);
        if (userOptional.isEmpty()) {
            throw new UserNotFoundException(id);
        }

        final User user = userOptional.get();
        final UserDto userDto = userMapper.toDto(user);

        final List<PaymentCardDto> cards = paymentCardDao.findAllByUserId(id)
                .stream()
                .map(paymentCardMapper::toDto)
                .collect(Collectors.toList());

        return new UserWithCardsDto(userDto, cards);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidUserDataException("Pageable cannot be null");
        }

        return userDao.findAll(pageable)
                .map(userMapper::toDto);
    }

    @Caching(evict = {
            @CacheEvict(value = "userWithCards", key = "#userDto.id"),
            @CacheEvict(value = "userWithCards", allEntries = true)
    })
    public UserDto updateUser(UserDto userDto) {
        if (userDto == null) {
            throw new InvalidUserDataException("User data with ID is required");
        }

        final Long userDtoId = userDto.getId();
        final Optional<User> userOptional = userDao.findUserById(userDtoId);

        if (userOptional.isPresent()) {
            final User bdUser = userOptional.get();
            userMapper.updateUserFromDto(userDto, bdUser);
            userDao.updateUserById(bdUser);

            return userMapper.toDto(bdUser);
        } else {
            throw new UserNotFoundException(userDto.getId());
        }
    }

    @CacheEvict(value = "userWithCards", key = "#id")
    public boolean activateUser(Long id) {
        if (id == null) {
            throw new InvalidUserDataException("ID cannot be null");
        }

        final int updated = userDao.activateUserById(id);

        return updated != 0;
    }

    @CacheEvict(value = "userWithCards", key = "#id")
    public boolean deactivateUser(Long id) {
        if (id == null) {
            throw new InvalidUserDataException("ID cannot be null");
        }

        final int updated = userDao.deactivateUserById(id);

        return updated != 0;
    }
}