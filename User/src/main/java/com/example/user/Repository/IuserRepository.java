package com.example.user.Repository;

import com.example.user.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IuserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
}
