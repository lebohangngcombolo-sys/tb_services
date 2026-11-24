package com.tradebridge.identity.repository;

import com.tradebridge.identity.domain.User;
import com.tradebridge.identity.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatus(@Param("status") UserStatus status, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<User> search(@Param("term") String term, Pageable pageable);
}
