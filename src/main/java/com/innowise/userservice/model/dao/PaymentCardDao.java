package com.innowise.userservice.model.dao;

import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.query.PaymentCardQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardDao extends JpaRepository<PaymentCard, Long> {
    int MAX_CARDS_PER_USER = 5;

    @Transactional
    default PaymentCard savePaymentCard(PaymentCard paymentCard) {
        final Long userId = paymentCard.getUser().getId();
        final long userCardsValue = countPaymentCardByUserId(userId);

        if (userCardsValue >= MAX_CARDS_PER_USER) {
            return null;
        }

        return save(paymentCard);
    }

    @Transactional(readOnly = true)
    Optional<PaymentCard> findPaymentCardById(Long id);

    @Transactional(readOnly = true)
    List<PaymentCard> findAll(Specification<PaymentCard> specification, Pageable pageable);

    @Transactional(readOnly = true)
    List<PaymentCard> findAllByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = PaymentCardQuery.UPDATE_PAYMENT_CARD_BY_ID_JPQL)
    void updatePaymentCardById(@Param("paymentCard") PaymentCard paymentCard);

    @Modifying
    @Transactional
    @Query(value = PaymentCardQuery.ACTIVATE_PAYMENT_CARD_BY_ID_JPQL)
    int activatePaymentCardById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = PaymentCardQuery.DEACTIVATE_PAYMENT_CARD_BY_ID_JPQL)
    int deactivatePaymentCardById(@Param("id") Long id);

    @Transactional(readOnly = true)
    @Query(value = PaymentCardQuery.COUNT_PAYMENT_CARD_BY_USER_ID_JPQL)
    long countPaymentCardByUserId(@Param("userId") Long userId);

    @Transactional(readOnly = true)
    boolean existsByNumber(String number);
}