package cl.duocuc.edutrack.ms.auth.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordService {

    private static final int COST = 12;

    public String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    public boolean verify(String rawPassword, String hash) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), hash).verified;
    }
}
