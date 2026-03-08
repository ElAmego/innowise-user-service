package com.innowise.userservice.model.dao;

import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.model.query.UserQuery;
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
public interface UserDao extends JpaRepository<User, Long> {

    @Modifying
    @Transactional
    @Query(value = UserQuery.SAVE_USER_NATIVE, nativeQuery = true)
    void saveUser(@Param("user") User user);

    @Transactional(readOnly = true)
    Optional<User> findUserById(Long id);

    @Transactional(readOnly = true)
    List<User> findAll(Specification<User> specification, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = UserQuery.UPDATE_USER_BY_ID_JPQL)
    void updateUserById(@Param("user") User user);

    @Modifying
    @Transactional
    @Query(value = UserQuery.ACTIVATE_USER_BY_ID_JPQL)
    int activateUserById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = UserQuery.DEACTIVATE_USER_BY_ID_JPQL)
    int deactivateUserById(@Param("id") Long id);
}