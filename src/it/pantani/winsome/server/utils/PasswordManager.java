/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * Classe che si occupa di fare l'hash di una password utilizzando la libreria esterna BCrypt.
 * Quest'ultima permette di hashare una stringa e di aggiungerci un salt senza doverlo gestire esplicitamente.
 */
public class PasswordManager {
    /**
     * Effettua l'hashing di una stringa
     * @param input la stringa di cui fare l'hash
     * @return la stringa hashata
     */
    public static String hashPassword(String input) {
        return BCrypt.hashpw(input, BCrypt.gensalt());
    }

    /**
     * Permette di verificare se la stringa fornita corrisponde al suo hash
     * @param plaintext la stringa da verificare
     * @param hashed la stringa salvata
     * @return true se la stringa corrisponde, false altrimenti
     */
    public static boolean checkPSW(String plaintext, String hashed) {
        return BCrypt.checkpw(plaintext, hashed);
    }
}
