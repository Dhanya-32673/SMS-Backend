package com.sicms.repository;

import com.sicms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    default Optional<User> findByEmailIgnoreCase(String email) {
        return findFirstByEmailIgnoreCaseOrderByIdAsc(email);
    }

    Optional<User> findFirstByGoogleSubjectOrderByIdAsc(String googleSubject);

    default Optional<User> findByGoogleSubject(String googleSubject) {
        return findFirstByGoogleSubjectOrderByIdAsc(googleSubject);
    }

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleRoleName(String roleName);
}
