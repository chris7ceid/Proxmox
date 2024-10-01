package org.ceid_uni.helpers;

import org.ceid_uni.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class Utils {

    private static Environment env;

    public Utils() {
    }

    public static SimpleMailMessage constructEmail(String subject, String body, User user) {
        final SimpleMailMessage email = new SimpleMailMessage();
        email.setSubject(subject);
        email.setText(body);
        email.setTo(user.getEmail());
        email.setFrom(env.getProperty("iris.app.support.email"));
        return email;
    }

    public static boolean isValidIds(String ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return ids.matches("(\\d+,)*\\d+");
    }

    @Autowired
    public void setEnv(Environment env) {
        Utils.env = env;
    }

}
