package org.ceid_uni.repository;

import jakarta.annotation.Nonnull;
import org.ceid_uni.models.Request;
import org.ceid_uni.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RequestsRepository extends JpaRepository<Request, Long> {
  @Nonnull
  List<Request> findByUser(User user);

  List<Request> findRequestsByCompleted(Boolean completed);

  @Override
  @Nonnull
  List<Request> findAll();
}
