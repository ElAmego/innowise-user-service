package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.exception.custom.DuplicatePaymentCardNumberException;
import com.innowise.userservice.exception.custom.InvalidPaymentCardDataException;
import com.innowise.userservice.exception.custom.PaymentCardLimitExceededException;
import com.innowise.userservice.exception.custom.PaymentCardNotFoundException;
import com.innowise.userservice.exception.custom.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
    private List<PaymentCard> cards;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@test.com");

        card = new PaymentCard();
        card.setId(1L);
        card.setNumber("1234567890123456");
        card.setHolder("John Doe");
        card.setExpirationDate(LocalDate.now().plusYears(3));
        card.setActive(true);
        card.setUser(user);

        cards = new ArrayList<>();
        cards.add(card);
        user.setPaymentCards(cards);

        cardDto = new PaymentCardDto();
        cardDto.setId(1L);
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("John Doe");
        cardDto.setExpirationDate(LocalDate.now().plusYears(3));
        cardDto.setActive(true);
    }

    @Test
    void createPaymentCard_Success_ReturnsCardDto() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        when(paymentCardDao.existsByNumber(anyString())).thenReturn(false);
        when(paymentCardMapper.toEntity(cardDto)).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final PaymentCardDto result = paymentCardService.createUserPaymentCard(1L, cardDto);

        assertNotNull(result);
        assertEquals("1234567890123456", result.getNumber());
        verify(userDao).findWithCardsById(1L);
        verify(paymentCardDao).existsByNumber(anyString());
        verify(paymentCardMapper).toEntity(cardDto);
        verify(userDao).save(user);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void createPaymentCard_UserNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.createUserPaymentCard(1L, cardDto));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void createPaymentCard_DuplicateNumber_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        when(paymentCardMapper.toEntity(cardDto)).thenReturn(card);
        when(paymentCardDao.existsByNumber(anyString())).thenReturn(true);

        assertThrows(DuplicatePaymentCardNumberException.class,
                () -> paymentCardService.createUserPaymentCard(1L, cardDto));

        verify(userDao).findWithCardsById(1L);
        verify(paymentCardDao).existsByNumber(anyString());
        verify(userDao, never()).save(any());
    }

    @Test
    void createPaymentCard_ExceedsLimit_ThrowsException() {
        User userWith5Cards = new User();
        userWith5Cards.setId(1L);
        List<PaymentCard> cards5 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            PaymentCard c = new PaymentCard();
            c.setId((long) i);
            cards5.add(c);
        }
        userWith5Cards.setPaymentCards(cards5);

        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(userWith5Cards));
        when(paymentCardMapper.toEntity(cardDto)).thenReturn(card);
        when(paymentCardDao.existsByNumber(anyString())).thenReturn(false);

        assertThrows(PaymentCardLimitExceededException.class,
                () -> paymentCardService.createUserPaymentCard(1L, cardDto));

        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void getUserPaymentCardById_Success_ReturnsCardDto() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final PaymentCardDto result = paymentCardService.getUserPaymentCardById(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("1234567890123456", result.getNumber());
        verify(userDao).findWithCardsById(1L);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void getUserPaymentCardById_UserNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.getUserPaymentCardById(1L, 1L));
        verify(userDao).findWithCardsById(1L);
    }

    @Test
    void getUserPaymentCardById_CardNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        assertThrows(PaymentCardNotFoundException.class,
                () -> paymentCardService.getUserPaymentCardById(1L, 999L));
        verify(userDao).findWithCardsById(1L);
    }

    @Test
    void getAllPaymentCardsByUserId_Success_ReturnsList() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        when(paymentCardMapper.toDto(card)).thenReturn(cardDto);

        final List<PaymentCardDto> result = paymentCardService.getAllPaymentCardsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userDao).findWithCardsById(1L);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void getAllPaymentCardsByUserId_UserNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.getAllPaymentCardsByUserId(1L));
        verify(userDao).findWithCardsById(1L);
    }

    @Test
    void updatePaymentCard_Success_ReturnsUpdatedCardDto() {
        final PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setId(1L);
        updateDto.setNumber("9876543210987654");
        updateDto.setHolder("Jane Doe");
        updateDto.setExpirationDate(LocalDate.now().plusYears(2));
        updateDto.setActive(false);

        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));
        doNothing().when(paymentCardMapper).updatePaymentCardFromDto(eq(updateDto), any(PaymentCard.class));
        when(paymentCardMapper.toDto(card)).thenReturn(updateDto);

        final PaymentCardDto result = paymentCardService.updateUserPaymentCard(1L, updateDto);

        assertNotNull(result);
        assertEquals(updateDto.getId(), result.getId());
        assertEquals(updateDto.getNumber(), result.getNumber());
        assertEquals(updateDto.getHolder(), result.getHolder());
        assertEquals(updateDto.getExpirationDate(), result.getExpirationDate());
        assertEquals(updateDto.getActive(), result.getActive());

        verify(userDao).findWithCardsById(1L);
        verify(paymentCardMapper).updatePaymentCardFromDto(eq(updateDto), eq(card));
        verify(userDao).save(user);
        verify(paymentCardMapper).toDto(card);
    }

    @Test
    void updatePaymentCard_UserNotFound_ThrowsException() {
        final PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setId(1L);

        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.updateUserPaymentCard(1L, updateDto));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void updatePaymentCard_CardNotFound_ThrowsException() {
        final PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setId(999L);

        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        assertThrows(PaymentCardNotFoundException.class,
                () -> paymentCardService.updateUserPaymentCard(1L, updateDto));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void updatePaymentCard_NullCardId_ThrowsException() {
        final PaymentCardDto updateDto = new PaymentCardDto();
        updateDto.setId(null);

        assertThrows(InvalidPaymentCardDataException.class,
                () -> paymentCardService.updateUserPaymentCard(1L, updateDto));
        verify(userDao, never()).findWithCardsById(any());
    }

    @Test
    void activatePaymentCard_Success_ReturnsTrue() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        final boolean result = paymentCardService.activateUserPaymentCard(1L, 1L);

        assertTrue(result);
        assertTrue(card.getActive());
        verify(userDao).findWithCardsById(1L);
        verify(userDao).save(user);
    }

    @Test
    void activatePaymentCard_UserNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.activateUserPaymentCard(1L, 1L));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void activatePaymentCard_CardNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        assertThrows(PaymentCardNotFoundException.class,
                () -> paymentCardService.activateUserPaymentCard(1L, 999L));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void deactivatePaymentCard_Success_ReturnsTrue() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        final boolean result = paymentCardService.deactivateUserPaymentCard(1L, 1L);

        assertTrue(result);
        assertFalse(card.getActive());
        verify(userDao).findWithCardsById(1L);
        verify(userDao).save(user);
    }

    @Test
    void deactivatePaymentCard_UserNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> paymentCardService.deactivateUserPaymentCard(1L, 1L));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }

    @Test
    void deactivatePaymentCard_CardNotFound_ThrowsException() {
        when(userDao.findWithCardsById(1L)).thenReturn(Optional.of(user));

        assertThrows(PaymentCardNotFoundException.class,
                () -> paymentCardService.deactivateUserPaymentCard(1L, 999L));
        verify(userDao).findWithCardsById(1L);
        verify(userDao, never()).save(any());
    }
}