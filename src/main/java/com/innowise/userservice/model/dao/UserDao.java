package com.innowise.userservice.model.dao;

import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.model.query.UserQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDao extends JpaRepository<User, Long> {

    @Modifying
    @Query(value = UserQuery.SAVE_USER_NATIVE, nativeQuery = true)
    Integer saveUser(@Param("user") User user);

    Optional<User> findUserById(Long id);

    Page<User> findAll(Specification<User> specification, Pageable pageable);

    @Modifying
    @Query(value = UserQuery.UPDATE_USER_BY_ID_JPQL)
    void updateUserById(@Param("user") User user);

    @Modifying
    @Query(value = UserQuery.ACTIVATE_USER_BY_ID_JPQL)
    int activateUserById(@Param("id") Long id);

    @Modifying
    @Query(value = UserQuery.DEACTIVATE_USER_BY_ID_JPQL)
    int deactivateUserById(@Param("id") Long id);

    boolean existsByEmail(String email);
}