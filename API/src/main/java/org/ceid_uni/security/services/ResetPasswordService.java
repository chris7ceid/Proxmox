package org.ceid_uni.security.services;

import org.ceid_uni.helpers.Utils;
import org.ceid_uni.models.ResetPassword;
import org.ceid_uni.models.User;
import org.ceid_uni.repository.ResetPasswordRepository;
import org.ceid_uni.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResetPasswordService implements IResetPasswordService {
    private ResetPasswordRepository resetPasswordRepository;
    private UserRepository userRepository;

    private JavaMailSender mailSender;

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordService.class);

    @Override
    public Optional<ResetPassword> findByToken(String token) {
        return resetPasswordRepository.findByToken(token);
    }

    @Override
    public Boolean existsByToken(String token) {
        return resetPasswordRepository.existsByToken(token);
    }

    @Override
    public Optional<ResetPassword> findByUser(User user) {
        return resetPasswordRepository.findByUser(user);
    }

    @Transactional
    @Override
    public String createToken(User user) {
        Optional<ResetPassword> passwordOptional = resetPasswordRepository.findByUser(user);

        return passwordOptional.map(ResetPassword::getToken)
                .orElseGet(() -> {
                    ResetPassword newPassword = new ResetPassword(user, UUID.randomUUID().toString(),
                            Instant.now().plusMillis(12000000));
                    resetPasswordRepository.save(newPassword);
                    return newPassword.getToken();
                });
    }

    @Transactional
    @Override
    public void changePassword(User user, String password) {
        user.setPassword(password);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void sendMail(User user) {
        String token;
        Optional<ResetPassword> passwordOptional = findByUser(user);
        if (passwordOptional.isPresent()) {
            logger.info("Valid token found inside the database. Use this.");
            token = passwordOptional.get().getToken();
        } else {
            logger.info("Valid token not found inside the database. Create one.");
            token = createToken(user);
        }
        mailSender.send(Utils.constructEmail("Reset password request",
                String.format(
                        "Dear User,\n\n" +
                                "We received a request to reset your password. To proceed, please follow the steps below:\n\n" +
                                "1. **POST Request**: Use the following endpoint to change your password:\n" +
                                "   - **URL**: http://<HostName>/api/auth/changepassword\n\n" +
                                "2. **Request Body**: Include the following JSON payload in the body of your POST request:\n" +
                                "   - **Payload**:\n" +
                                "     {\n" +
                                "       \"token\": \"%s\",\n" +
                                "       \"newPassword\": \"<your_new_password>\"\n" +
                                "     }\n\n" +
                                "   - **token**: The token provided here is used to verify the reset request.\n" +
                                "   - **newPassword**: Replace `<your_new_password>` with your new password. Make sure your new password matches with the password in Proxmox.\n\n" +
                                "If you did not request a password reset, please ignore this email.\n\n" +
                                "Best regards.\n",
                        token
                ), user));
    }

    @Transactional
    @Override
    public void deleteByUserId(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            resetPasswordRepository.deleteByUser(user);
        }
    }

    @Autowired
    public void setResetPasswordRepository(ResetPasswordRepository resetPasswordRepository) {
        this.resetPasswordRepository = resetPasswordRepository;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
}
