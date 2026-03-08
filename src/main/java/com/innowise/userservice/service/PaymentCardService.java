package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardDto;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dao.PaymentCardDao;
import com.innowise.userservice.model.entity.PaymentCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentCardService {
    private final PaymentCardDao paymentCardDao;
    private final PaymentCardMapper paymentCardMapper;

    @Autowired
    public PaymentCardService(PaymentCardDao paymentCardDao, PaymentCardMapper paymentCardMapper) {
        this.paymentCardDao = paymentCardDao;
        this.paymentCardMapper = paymentCardMapper;
    }

    public PaymentCardDto createPaymentCard(PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            return null;
        }

        final PaymentCard paymentCard = paymentCardMapper.toEntity(paymentCardDto);
        final String paymentCardNumber = paymentCard.getNumber();
        final boolean isExist = paymentCardDao.existsByNumber(paymentCardNumber);

        if (!isExist) {
            paymentCard.setActive(true);
            paymentCardDao.savePaymentCard(paymentCard);

            return paymentCardMapper.toDto(paymentCard);
        } else {
            return null;
        }
    }

    public PaymentCardDto getPaymentCardById(Long id) {
        if (id == null) {
            return null;
        }

        final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(id);

        if (paymentCardOptional.isPresent()) {
            final PaymentCard paymentCard = paymentCardOptional.get();

            return paymentCardMapper.toDto(paymentCard);
        }

        return null;
    }

    public Page<PaymentCardDto> getAllPaymentCards(Pageable pageable) {
        if (pageable == null) {
            return null;
        }

        return paymentCardDao.findAll(pageable)
                .map(paymentCardMapper::toDto);
    }

    public List<PaymentCardDto> getAllPaymentCardsByUserId(Long userId) {
        if (userId == null) {
            return null;
        }

        final List<PaymentCard> paymentCardList = paymentCardDao.findAllByUserId(userId);
        final List<PaymentCardDto> paymentCardDtoList = new ArrayList<>();

        for (final PaymentCard paymentCard: paymentCardList) {
            final PaymentCardDto paymentCardDto = paymentCardMapper.toDto(paymentCard);
            paymentCardDtoList.add(paymentCardDto);
        }

        return paymentCardDtoList;
    }

    public PaymentCardDto updatePaymentCard(PaymentCardDto paymentCardDto) {
        if (paymentCardDto == null) {
            return null;
        }

        final Long paymentCardDtoId = paymentCardDto.getId();
        final Optional<PaymentCard> paymentCardOptional = paymentCardDao.findPaymentCardById(paymentCardDtoId);

        if (paymentCardOptional.isPresent()) {
            final PaymentCard paymentCard = paymentCardOptional.get();
            paymentCardDao.updatePaymentCardById(paymentCard);

            return paymentCardMapper.toDto(paymentCard);
        }

        return null;
    }

    public boolean activatePaymentCard(Long id) {
        if (id == null) {
            return false;
        }

        final int updated = paymentCardDao.activatePaymentCardById(id);

        return updated != 0;
    }

    public boolean deactivatePaymentCard(Long id) {
        if (id == null) {
            return false;
        }

        final int updated = paymentCardDao.deactivatePaymentCardById(id);

        return updated != 0;
    }
}