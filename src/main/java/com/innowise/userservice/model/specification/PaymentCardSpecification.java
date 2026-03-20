package com.innowise.userservice.model.specification;

import com.innowise.userservice.model.entity.PaymentCard;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PaymentCardSpecification {

    public static Specification<PaymentCard> numberLike(String number) {
        return (root, query, criteriaBuilder) -> {
            if (number == null || number.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.like(root.get("number"), "%" + number + "%");
        };
    }
}