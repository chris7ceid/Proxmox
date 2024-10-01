package org.ceid_uni.repository;

import jakarta.annotation.Nonnull;
import org.ceid_uni.models.VmDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VmDetailsRepository extends JpaRepository<VmDetails, Long> {
  VmDetails findByVmId(Long id);
  @Override
  @Nonnull
  List<VmDetails> findAll();
}
