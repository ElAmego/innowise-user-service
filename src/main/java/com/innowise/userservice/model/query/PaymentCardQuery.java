package com.innowise.userservice.model.query;

public final class PaymentCardQuery {
    public static final String UPDATE_PAYMENT_CARD_BY_ID_JPQL = """
            UPDATE PaymentCard pc
            SET pc.number = :#{#paymentCard.number},
            pc.holder = :#{#paymentCard.holder},
            pc.expirationDate = :#{#paymentCard.expirationDate},
            pc.active = :#{#paymentCard.active},
            pc.updatedAt = CURRENT_TIMESTAMP
            WHERE pc.id = :#{#paymentCard.id}
            """;

    public static final String ACTIVATE_PAYMENT_CARD_BY_ID_JPQL = """
            UPDATE PaymentCard pc
            SET pc.active = true, pc.updatedAt = CURRENT_TIMESTAMP
            WHERE pc.id = :id
            """;

    public static final String DEACTIVATE_PAYMENT_CARD_BY_ID_JPQL = """
            UPDATE PaymentCard pc
            SET pc.active = false, pc.updatedAt = CURRENT_TIMESTAMP
            WHERE pc.id = :id
            """;

    public static final String COUNT_PAYMENT_CARD_BY_USER_ID_JPQL = """
            SELECT COUNT(pc)
            FROM PaymentCard pc
            WHERE pc.user.id = :userId
            """;

    private PaymentCardQuery() { }
}
