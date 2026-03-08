package com.innowise.userservice.model.specification;

import com.innowise.userservice.model.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public final class PaymentCartSpecification {

    private PaymentCartSpecification() { }

    public static Specification<PaymentCard> numberLike(String number) {
        return (root, query, criteriaBuilder) -> {
            if (number == null || number.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(root.get("number"), "%" + number + "%");
        };
    }
}