package com.innowise.userservice.model.dao;

import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.query.PaymentCardQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardDao extends JpaRepository<PaymentCard, Long> {
    PaymentCard save(PaymentCard paymentCard);

    Optional<PaymentCard> findPaymentCardById(Long id);

    Page<PaymentCard> findAll(Specification<PaymentCard> specification, Pageable pageable);

    List<PaymentCard> findAllByUserId(Long userId);

    @Modifying
    @Query(value = PaymentCardQuery.UPDATE_PAYMENT_CARD_BY_ID_JPQL)
    void updatePaymentCardById(@Param("paymentCard") PaymentCard paymentCard);

    @Modifying
    @Query(value = PaymentCardQuery.ACTIVATE_PAYMENT_CARD_BY_ID_JPQL)
    int activatePaymentCardById(@Param("id") Long id);

    @Modifying
    @Query(value = PaymentCardQuery.DEACTIVATE_PAYMENT_CARD_BY_ID_JPQL)
    int deactivatePaymentCardById(@Param("id") Long id);

    @Query(value = PaymentCardQuery.COUNT_PAYMENT_CARD_BY_USER_ID_JPQL)
    long countPaymentCardByUserId(@Param("userId") Long userId);

    boolean existsByNumber(String number);

    boolean existsByIdAndUserId(Long cardId, Long userId);
}