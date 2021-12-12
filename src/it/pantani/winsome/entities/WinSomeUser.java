/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import it.pantani.winsome.utils.PasswordManager;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WinSomeUser {
    private static int lastID = -1; // ultimo id dell'utente salvato

    private final int id;
    private String username;
    private char[] password;
    private ConcurrentLinkedQueue<String> tags_list = new ConcurrentLinkedQueue<>();

    public WinSomeUser(String username, String password, List<String> tags_list) {
        lastID++;
        id = lastID;
        this.username = username;
        this.password = PasswordManager.hashPassword(password);

        for(String tag : tags_list) {
            this.tags_list.add(tag.toLowerCase());
        }
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public ConcurrentLinkedQueue<String> getTags_list() {
        return tags_list;
    }

    public char[] getPassword() {
        return password;
    }

    public static void setLastID(int updated_lastID) {
        lastID = updated_lastID;
    }
}
