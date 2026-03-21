package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.exception.custom.*;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.model.specification.PaymentCardSpecification;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentCardService {
    private static final int MAX_CARDS_PER_USER = 5;
    private final PaymentCardDao paymentCardDao;
    private final UserDao userDao;
    private final PaymentCardMapper paymentCardMapper;

    @CacheEvict(value = "userWithCards", key = "#userId")
    public PaymentCardDto createUserPaymentCard(Long userId, PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            throw new InvalidPaymentCardDataException("Payment card data cannot be null");
        }

        if (userId == null) {
            throw new InvalidPaymentCardDataException("User ID is required");
        }

        final Optional<User> userOptional = userDao.findWithCardsById(userId);

        if (userOptional.isPresent()) {
            final PaymentCard paymentCard = paymentCardMapper.toEntity(paymentCardDto);
            final String paymentCardNumber = paymentCard.getNumber();
            final boolean isExist = paymentCardDao.existsByNumber(paymentCardNumber);

            if (!isExist) {
                final User user = userOptional.get();
                final int paymentCardsLength = user.getPaymentCards().size();

                if (paymentCardsLength < MAX_CARDS_PER_USER) {
                    paymentCard.setActive(true);
                    user.addPaymentCard(paymentCard);
                    userDao.save(user);

                    return paymentCardMapper.toDto(paymentCard);
                } else {
                    throw new PaymentCardLimitExceededException(userId, paymentCardsLength, MAX_CARDS_PER_USER);
                }

            } else {
                throw new DuplicatePaymentCardNumberException(paymentCardDto.getNumber());
            }
        } else {
            throw new UserNotFoundException(userId);
        }
    }

    @Transactional(readOnly = true)
    public PaymentCardDto getUserPaymentCardById(Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("userID cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("cardID cannot be null");
        }

        final User user = userDao.findWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        final PaymentCard paymentCard = user.getPaymentCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new PaymentCardNotFoundException(cardId));

        return paymentCardMapper.toDto(paymentCard);
    }

    @Transactional(readOnly = true)
    public Page<PaymentCardDto> getAllPaymentCardsSpecification(String number, Pageable pageable) {
        final Specification<PaymentCard> paymentCardSpecification = PaymentCardSpecification.numberLike(number);
        return paymentCardDao.findAll(paymentCardSpecification, pageable)
                .map(paymentCardMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<PaymentCardDto> getAllPaymentCardsByUserId(Long userId) {
        if (userId == null) {
            throw new InvalidUserDataException("User ID cannot be null");
        }

        final User user = userDao.findWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getPaymentCards().stream()
                .map(paymentCardMapper::toDto)
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = "userWithCards", key = "#userId")
    })
    public PaymentCardDto updateUserPaymentCard(Long userId, PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            throw new InvalidPaymentCardDataException("Payment card data cannot be null");
        }

        final Long cardId = paymentCardDto.getId();

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("Card ID cannot be null");
        }

        if (userId == null) {
            throw new InvalidUserDataException("userId cannot be null");
        }

        final User user = userDao.findWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        final PaymentCard existingCard = user.getPaymentCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new PaymentCardNotFoundException(cardId));

        paymentCardMapper.updatePaymentCardFromDto(paymentCardDto, existingCard);
        userDao.save(user);

        return paymentCardMapper.toDto(existingCard);
    }

    public boolean activateUserPaymentCard(Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("userId cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("cardId cannot be null");
        }

        final User user = userDao.findWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        final PaymentCard paymentCard = user.getPaymentCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new PaymentCardNotFoundException(cardId));

        paymentCard.setActive(true);
        userDao.save(user);
        evictUserWithCardsCache(userId);

        return true;
    }

    public boolean deactivateUserPaymentCard(Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("userId cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("cardId cannot be null");
        }

        final User user = userDao.findWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        final PaymentCard paymentCard = user.getPaymentCards().stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new PaymentCardNotFoundException(cardId));

        paymentCard.setActive(false);
        userDao.save(user);
        evictUserWithCardsCache(userId);

        return true;
    }

    @CacheEvict(value = "userWithCards", key = "#userId")
    private void evictUserWithCardsCache(Long userId) { }
}