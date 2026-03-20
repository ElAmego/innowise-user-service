package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.exception.custom.*;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.model.specification.PaymentCardSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentCardService {
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

        final Optional<User> userOptional = userDao.findUserById(userId);

        if (userOptional.isPresent()) {
            final PaymentCard paymentCard = paymentCardMapper.toEntity(paymentCardDto);
            final String paymentCardNumber = paymentCard.getNumber();
            final boolean isExist = paymentCardDao.existsByNumber(paymentCardNumber);

            if (!isExist) {
                final User user = userOptional.get();
                paymentCard.setActive(true);
                paymentCard.setUser(user);
                final PaymentCard addedPaymentCard = paymentCardDao.savePaymentCard(paymentCard);

                return paymentCardMapper.toDto(addedPaymentCard);
            } else {
                throw new DuplicatePaymentCardNumberException(paymentCardDto.getNumber());
            }
        } else {
            throw new UserNotFoundException(userId);
        }
    }

    @Transactional(readOnly = true)
    public PaymentCardDto getUserPaymentCardById(final Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("userID cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("cardID cannot be null");
        }

        final boolean isMatch = paymentCardDao.existsByIdAndUserId(cardId, userId);

        if (isMatch) {
            final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(cardId);

            if (paymentCardOptional.isPresent()) {
                final PaymentCard paymentCard = paymentCardOptional.get();

                return paymentCardMapper.toDto(paymentCard);
            } else {
                throw new PaymentCardNotFoundException(cardId);
            }

        } else {
            throw new PaymentCardNotFoundException("User's payment card wasn't find");
        }
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

        if (userDao.existsById(userId)) {
            final List<PaymentCard> paymentCardList = paymentCardDao.findAllByUserId(userId);
            final List<PaymentCardDto> paymentCardDtoList = new ArrayList<>();

            for (final PaymentCard paymentCard: paymentCardList) {
                final PaymentCardDto paymentCardDto = paymentCardMapper.toDto(paymentCard);
                paymentCardDtoList.add(paymentCardDto);
            }

            return paymentCardDtoList;
        } else {
            throw new UserNotFoundException(userId);
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "userWithCards", key = "#userId")
    })
    public PaymentCardDto updateUserPaymentCard(Long userId, PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            throw new InvalidPaymentCardDataException("Payment card data cannot be null");
        }

        if (userId == null) {
            throw new InvalidUserDataException("userId cannot be null");
        }

        final Long paymentCardDtoId = paymentCardDto.getId();
        final boolean isMatch = paymentCardDao.existsByIdAndUserId(paymentCardDtoId, userId);

        if (isMatch) {
            final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(paymentCardDtoId);

            if (paymentCardOptional.isPresent()) {
                final PaymentCard bdPaymentCard = paymentCardOptional.get();

                paymentCardMapper.updatePaymentCardFromDto(paymentCardDto, bdPaymentCard);
                paymentCardDao.updatePaymentCardById(bdPaymentCard);

                return paymentCardMapper.toDto(bdPaymentCard);
            } else {
                throw new PaymentCardNotFoundException(paymentCardDto.getId());
            }
        } else {
            throw new PaymentCardNotFoundException("User's payment card wasn't find");
        }
    }

    public boolean activateUserPaymentCard(Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("userId cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("cardId cannot be null");
        }

        final boolean isMatch = paymentCardDao.existsByIdAndUserId(cardId, userId);

        if (isMatch) {
            final boolean isUpdated = paymentCardDao.activatePaymentCardById(cardId) != 0;

            if (isUpdated) {
                evictUserWithCardsCache(userId);
            }

            return isUpdated;
        } else {
            throw new PaymentCardNotFoundException("User's payment card wasn't find");
        }
    }

    public boolean deactivateUserPaymentCard(Long userId, Long cardId) {
        if (userId == null) {
            throw new InvalidUserDataException("ID cannot be null");
        }

        if (cardId == null) {
            throw new InvalidPaymentCardDataException("ID cannot be null");
        }

        final boolean isMatch = paymentCardDao.existsByIdAndUserId(cardId, userId);

        if (isMatch) {
            final boolean isUpdated = paymentCardDao.deactivatePaymentCardById(cardId) != 0;

            if (isUpdated) {
                evictUserWithCardsCache(userId);
            }

            return isUpdated;
        } else {
            throw new PaymentCardNotFoundException("User's payment card wasn't find");
        }
    }

    @CacheEvict(value = "userWithCards", key = "#userId")
    private void evictUserWithCardsCache(Long userId) { }
}