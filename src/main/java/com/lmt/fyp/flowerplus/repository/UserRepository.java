package com.lmt.fyp.flowerplus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lmt.fyp.flowerplus.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
     User findByEmail(String email);
}
