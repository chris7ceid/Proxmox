package org.ceid_uni.repository;

import jakarta.annotation.Nonnull;
import org.ceid_uni.models.ERole;
import org.ceid_uni.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);

  @Override
  @Nonnull
  List<Role> findAll();
}
