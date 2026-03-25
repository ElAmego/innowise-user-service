package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.exception.custom.DuplicateEmailException;
import com.innowise.userservice.exception.custom.InvalidUserDataException;
import com.innowise.userservice.exception.custom.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserDao userDao;

    @Mock
    private PaymentCardDao paymentCardDao;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDto userDto;
    private PaymentCard card;
    private PaymentCardDto cardDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@test.com");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setActive(true);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John");
        userDto.setSurname("Doe");
        userDto.setEmail("john@test.com");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setActive(true);

        card = new PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setUser(user);

        cardDto = new PaymentCardDto();
        cardDto.setId(1L);
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("John Doe");
        cardDto.setExpirationDate(LocalDate.now().plusYears(3));
        cardDto.setActive(true);
    }

    @Test
    void createUser_Success_ReturnsTrue() {
        when(userDao.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(user);
        when(userDao.saveUser(any(User.class))).thenReturn(1);

        final boolean result = userService.createUser(userDto);

        assertTrue(result);
        verify(userDao).saveUser(user);
        verify(userDao).existsByEmail(userDto.getEmail());
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(user);
        when(userDao.existsByEmail(anyString())).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(userDto));

        verify(userDao, never()).saveUser(any());
    }

    @Test
    void createUser_NullDto_ThrowsException() {
        assertThrows(InvalidUserDataException.class, () -> userService.createUser(null));
    }

    @Test
    void getUserById_Success_ReturnsUserDto() {
        when(userDao.findUserById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        final UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@test.com", result.getEmail());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userDao.findUserById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getUserWithCardsById_Success_ReturnsUserWithCardsDto() {
        UserDto userDtoWithCards = new UserDto();
        userDtoWithCards.setId(1L);
        userDtoWithCards.setName("John");
        userDtoWithCards.setSurname("Doe");
        userDtoWithCards.setEmail("john@test.com");
        userDtoWithCards.setBirthDate(LocalDate.of(1990, 1, 1));
        userDtoWithCards.setActive(true);

        PaymentCardDto cardDto = new PaymentCardDto();
        cardDto.setId(1L);
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("John Doe");
        cardDto.setExpirationDate(LocalDate.now().plusYears(3));
        cardDto.setActive(true);

        userDtoWithCards.setCards(List.of(cardDto));

        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDtoWithCards(user)).thenReturn(userDtoWithCards);

        final UserDto result = userService.getUserWithCardsById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getCards());
        assertEquals(1, result.getCards().size());
        assertEquals("1234567890123456", result.getCards().get(0).getNumber());

        verify(userDao).findWithCardsById(1L);
        verify(userMapper).toDtoWithCards(user);
        verifyNoMoreInteractions(userDao, userMapper);
    }

    @Test
    void updateUser_Success_ReturnsUpdatedUserDto() {
        final UserDto updateDto = new UserDto();
        updateDto.setId(1L);
        updateDto.setName("John Updated");
        updateDto.setSurname("Doe Updated");
        updateDto.setBirthDate(LocalDate.of(1990, 1, 1));
        updateDto.setEmail("john.updated@test.com");
        updateDto.setActive(true);

        when(userDao.findUserById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userMapper).updateUserFromDto(eq(updateDto), any(User.class));
        doNothing().when(userDao).updateUserById(any(User.class));
        when(userMapper.toDto(user)).thenReturn(updateDto);

        final UserDto result = userService.updateUser(updateDto);

        assertNotNull(result);
        assertEquals("John Updated", result.getName());
        assertEquals("john.updated@test.com", result.getEmail());

        verify(userDao).findUserById(1L);
        verify(userMapper).updateUserFromDto(eq(updateDto), any(User.class));
        verify(userDao).updateUserById(any(User.class));
        verify(userMapper).toDto(user);
    }

    @Test
    void activateUser_Success_ReturnsTrue() {
        when(userDao.activateUserById(1L)).thenReturn(1);

        final boolean result = userService.activateUser(1L);

        assertTrue(result);
    }

    @Test
    void activateUser_NotFound_ReturnsFalse() {
        when(userDao.activateUserById(99L)).thenReturn(0);

        final boolean result = userService.activateUser(99L);

        assertFalse(result);
    }

    @Test
    void getAllUsers_Success_ReturnsPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        final Page<User> userPage = new PageImpl<>(List.of(user));

        when(userDao.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userDto);

        final Page<UserDto> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_NullPageable_ThrowsException() {
        assertThrows(InvalidUserDataException.class, () -> userService.getAllUsers(null));
    }

    @Test
    void findAll_WithNameAndSurname_ReturnsFilteredPage() {
        final Pageable pageable = PageRequest.of(0, 10);
        final String name = "John";
        final String surname = "Doe";
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userDao.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userDto);

        final Page<UserDto> result = userService.findAll(name, surname, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getName());
        verify(userDao).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void deactivateUser_Success_ReturnsTrue() {
        when(userDao.deactivateUserById(1L)).thenReturn(1);

        final boolean result = userService.deactivateUser(1L);

        assertTrue(result);
        verify(userDao).deactivateUserById(1L);
    }
}