package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.dto.UserDto;
import com.innowise.userservice.exception.custom.DuplicatePaymentCardNumberException;
import com.innowise.userservice.exception.custom.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceUnitTest {

    @Mock
    private PaymentCardDao paymentCardDao;

    @Mock
    private UserDao userDao;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User user;
    private PaymentCard card;
    private PaymentCardDto cardDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@test.com");

        UserDto userDto = new UserDto();
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
    void createPaymentCard_Success_ReturnsCardDto() {
        when(userDao.findUserById(1L)).thenReturn(Optional.of(user));
        when(paymentCardDao.existsByNumber(anyString())).thenReturn(false);
        when(paymentCardMapper.toEntity(cardDto)).thenReturn(card);
        when(paymentCardDao.savePaymentCard(any(PaymentCard.class))).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final PaymentCardDto result = paymentCardService.createUserPaymentCard(1L, cardDto);

        assertNotNull(result);
        assertEquals("1234567890123456", result.getNumber());
        verify(paymentCardDao).savePaymentCard(card);
    }

    @Test
    void createPaymentCard_UserNotFound_ThrowsException() {
        when(userDao.findUserById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> paymentCardService.createUserPaymentCard(1L, cardDto));
    }

    @Test
    void createPaymentCard_DuplicateNumber_ThrowsException() {
        when(paymentCardMapper.toEntity(any(PaymentCardDto.class))).thenReturn(card);
        when(userDao.findUserById(1L)).thenReturn(Optional.of(user));
        when(paymentCardDao.existsByNumber(anyString())).thenReturn(true);

        assertThrows(DuplicatePaymentCardNumberException.class,
                () -> paymentCardService.createUserPaymentCard(1L, cardDto));
    }

    @Test
    void getPaymentCardById_Success_ReturnsCardDto() {
        when(paymentCardDao.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        when(paymentCardDao.findPaymentCardById(1L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final PaymentCardDto result = paymentCardService.getUserPaymentCardById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("1234567890123456", result.getNumber());
        verify(paymentCardDao).existsByIdAndUserId(1L, 1L);
        verify(paymentCardDao).findPaymentCardById(1L);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void getAllPaymentCardsByUserId_Success_ReturnsList() {
        final List<PaymentCard> cards = List.of(card);
        when(paymentCardDao.findAllByUserId(1L)).thenReturn(cards);
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final List<PaymentCardDto> result = paymentCardService.getAllPaymentCardsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void updatePaymentCard_Success_ReturnsUpdatedCardDto() {
        final PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setId(1L);
        updateDto.setNumber("9876543210987654");
        updateDto.setHolder("Jane Doe");
        updateDto.setExpirationDate(LocalDate.now().plusYears(2));
        updateDto.setActive(true);

        when(paymentCardDao.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        when(paymentCardDao.findPaymentCardById(1L)).thenReturn(Optional.of(card));
        doNothing().when(paymentCardMapper).updatePaymentCardFromDto(eq(updateDto), any(PaymentCard.class));
        doNothing().when(paymentCardDao).updatePaymentCardById(any(PaymentCard.class));
        when(paymentCardMapper.toDto(card)).thenReturn(updateDto);

        final PaymentCardDto result = paymentCardService.updateUserPaymentCard(1L, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getId(), result.getId());
        assertEquals(updateDto.getNumber(), result.getNumber());
        assertEquals(updateDto.getHolder(), result.getHolder());
        assertEquals(updateDto.getExpirationDate(), result.getExpirationDate());
        assertEquals(updateDto.getActive(), result.getActive());

        verify(paymentCardDao).existsByIdAndUserId(1L, 1L);
        verify(paymentCardDao).findPaymentCardById(1L);
        verify(paymentCardMapper).updatePaymentCardFromDto(eq(updateDto), eq(card));
        verify(paymentCardDao).updatePaymentCardById(card);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void activatePaymentCard_Success_ReturnsTrue() {
        when(paymentCardDao.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        when(paymentCardDao.activatePaymentCardById(1L)).thenReturn(1);

        final boolean result = paymentCardService.activateUserPaymentCard(1L, 1L);

        assertTrue(result);
        verify(paymentCardDao).existsByIdAndUserId(1L, 1L);
        verify(paymentCardDao).activatePaymentCardById(1L);
    }

    @Test
    void deactivatePaymentCard_Success_ReturnsTrue() {
        when(paymentCardDao.existsByIdAndUserId(1L, 1L)).thenReturn(true);
        when(paymentCardDao.deactivatePaymentCardById(1L)).thenReturn(1);

        final boolean result = paymentCardService.deactivateUserPaymentCard(1L, 1L);

        assertTrue(result);
        verify(paymentCardDao).existsByIdAndUserId(1L, 1L);
        verify(paymentCardDao).deactivatePaymentCardById(1L);
    }
}