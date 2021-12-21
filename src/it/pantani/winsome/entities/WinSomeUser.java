/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import it.pantani.winsome.utils.PasswordManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WinSomeUser {
    private final String username;
    private final String password;
    private final Set<String> tags_list = new LinkedHashSet<>(); // mantiene l'ordine di inserimento ed impedisce che due elementi uguali vengano inseriti
    private final long creationDate;

    public WinSomeUser(String username, String password, List<String> tags_list) {
        this.username = username;
        this.password = PasswordManager.hashPassword(password);

        for(String tag : tags_list) {
            this.tags_list.add(tag.toLowerCase());
        }

        this.creationDate = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public Set<String> getTags_list() {
        return tags_list;
    }

    public boolean checkPassword(String password) {
        return PasswordManager.checkPSW(password, String.valueOf(this.password));
    }
}
