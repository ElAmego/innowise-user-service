package com.innowise.userservice.model.query;

public final class UserQuery {
    public static final String SAVE_USER_NATIVE = """
            INSERT INTO users (name, surname, email, birth_date, active, created_at, updated_at)
            VALUES (:#{#user.name}, :#{#user.surname}, :#{#user.email}, :#{#user.birthDate}, :#{#user.active}, 
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

    public static final String UPDATE_USER_BY_ID_JPQL = """
            UPDATE User u
            SET u.name = :#{#user.name},
            u.surname = :#{#user.surname},
            u.email = :#{#user.email},
            u.birthDate = :#{#user.birthDate},
            u.active = :#{#user.active},
            u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :#{#user.id}
            """;

    public static final String ACTIVATE_USER_BY_ID_JPQL = """
            UPDATE User u
            SET u.active = true, u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :id
            """;

    public static final String DEACTIVATE_USER_BY_ID_JPQL = """
            UPDATE User u
            SET u.active = false, u.updatedAt = CURRENT_TIMESTAMP
            WHERE u.id = :id
            """;

    private UserQuery() { }
}