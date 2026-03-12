package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.exception.custom.DuplicatePaymentCardNumberException;
import com.innowise.userservice.exception.custom.InvalidPaymentCardDataException;
import com.innowise.userservice.exception.custom.PaymentCardNotFoundException;
import com.innowise.userservice.exception.custom.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.dao.UserDao;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentCardService {
    private final PaymentCardDao paymentCardDao;
    private final UserDao userDao;
    private final PaymentCardMapper paymentCardMapper;

    @Autowired
    public PaymentCardService(PaymentCardDao paymentCardDao, UserDao userDao, PaymentCardMapper paymentCardMapper) {
        this.paymentCardDao = paymentCardDao;
        this.userDao = userDao;
        this.paymentCardMapper = paymentCardMapper;
    }

    @CacheEvict(value = "userWithCards", key = "#paymentCardDto.user.id")
    public PaymentCardDto createPaymentCard(PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            throw new InvalidPaymentCardDataException("Payment card data cannot be null");
        }

        final Long userId = paymentCardDto.getUser().getId();

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
            throw new UserNotFoundException(paymentCardDto.getUser().getId());
        }
    }

    public PaymentCardDto getPaymentCardById(Long id) {
        if (id == null) {
            throw new InvalidPaymentCardDataException("ID cannot be null");
        }

        final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(id);

        if (paymentCardOptional.isPresent()) {
            final PaymentCard paymentCard = paymentCardOptional.get();

            return paymentCardMapper.toDto(paymentCard);
        } else {
            throw new PaymentCardNotFoundException(id);
        }
    }

    public Page<PaymentCardDto> getAllPaymentCards(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidPaymentCardDataException("Pageable cannot be null");
        }

        return paymentCardDao.findAll(pageable)
                .map(paymentCardMapper::toDto);
    }

    public List<PaymentCardDto> getAllPaymentCardsByUserId(Long userId) {
        if (userId == null) {
            throw new InvalidPaymentCardDataException("User ID cannot be null");
        }

        final List<PaymentCard> paymentCardList = paymentCardDao.findAllByUserId(userId);
        final List<PaymentCardDto> paymentCardDtoList = new ArrayList<>();

        for (final PaymentCard paymentCard: paymentCardList) {
            final PaymentCardDto paymentCardDto = paymentCardMapper.toDto(paymentCard);
            paymentCardDtoList.add(paymentCardDto);
        }

        return paymentCardDtoList;
    }

    @Caching(evict = {
            @CacheEvict(value = "userWithCards", key = "#paymentCardDto.user.id")
    })
    public PaymentCardDto updatePaymentCard(PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            throw new InvalidPaymentCardDataException("Payment card data cannot be null");
        }

        final Long paymentCardDtoId = paymentCardDto.getId();
        final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(paymentCardDtoId);

        if (paymentCardOptional.isPresent()) {
            final PaymentCard bdPaymentCard = paymentCardOptional.get();
            paymentCardMapper.updatePaymentCardFromDto(paymentCardDto, bdPaymentCard);
            paymentCardDao.updatePaymentCardById(bdPaymentCard);

            return paymentCardMapper.toDto(bdPaymentCard);
        } else {
            throw new PaymentCardNotFoundException(paymentCardDto.getId());
        }
    }

    @CacheEvict(value = "userWithCards", key = "#id")
    public boolean activatePaymentCard(Long id) {
        if (id == null) {
            throw new InvalidPaymentCardDataException("ID cannot be null");
        }

        final int updated = paymentCardDao.activatePaymentCardById(id);

        return updated != 0;
    }

    @CacheEvict(value = "userWithCards", key = "#id")
    public boolean deactivatePaymentCard(Long id) {
        if (id == null) {
            throw new InvalidPaymentCardDataException("ID cannot be null");
        }

        final int updated = paymentCardDao.deactivatePaymentCardById(id);

        return updated != 0;
    }
}