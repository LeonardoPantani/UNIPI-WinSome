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

    public static boolean checkPSW(String password, String hashed) {
        return BCrypt.checkpw(password, hashed);
    }

    /*
    public static char[] getNextSalt() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789";
        Random random = new SecureRandom();
        int randomIndex, randomChar;

        char[] salt = new char[16];
        for(int i = 0; i < salt.length; i++) {
            randomIndex = random.nextInt(alphabet.length());
            randomChar = alphabet.charAt(randomIndex);
            salt[i] = (char)randomChar;
        }

        return salt;
    }
     */
}
