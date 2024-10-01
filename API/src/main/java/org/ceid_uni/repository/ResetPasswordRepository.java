package org.ceid_uni.repository;

import org.ceid_uni.models.ResetPassword;
import org.ceid_uni.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResetPasswordRepository extends JpaRepository<ResetPassword, Long> {
  Optional<ResetPassword> findByToken(String token);
  Optional<ResetPassword> findByUser(User user);

  Boolean existsByToken(String token);

  @Modifying
  void deleteByUser(User user);
}
