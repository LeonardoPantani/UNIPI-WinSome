/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

public class PasswordManager {
    public static char[] hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt()).toCharArray();

    }

    public static boolean checkPSW(String plaintext, String hashed) {
        return BCrypt.checkpw(plaintext, hashed);
    }
}
