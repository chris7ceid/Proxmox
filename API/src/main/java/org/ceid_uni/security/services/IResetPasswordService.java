package org.ceid_uni.security.services;

import org.ceid_uni.models.ResetPassword;
import org.ceid_uni.models.User;

import java.util.Optional;

public interface IResetPasswordService {
    Optional<ResetPassword> findByToken(String token);

    Boolean existsByToken(String token);

    Optional<ResetPassword> findByUser(User user);

    String createToken(User user);

    void changePassword(User user, String password);

    void deleteByUserId(Long userId);

    void sendMail(User user);


}
