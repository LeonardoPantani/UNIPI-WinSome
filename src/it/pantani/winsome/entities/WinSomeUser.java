/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Classe che rappresenta un utente WinSome. Di ogni utente ci interessano il suo nome, la sua password (hashata),
 * la lista di tags (senza limite qui, viene imposto dalla classe che usa questo oggetto) e la data di creazione
 * dell'account.
 */
public class WinSomeUser {
    private final String username;
    private final String password;
    private final Set<String> tags_list = new LinkedHashSet<>(); // mantiene l'ordine di inserimento ed impedisce che due elementi uguali vengano inseriti
    private final long creationDate;

    /**
     * Questo costruttore inizializza un utente WinSome. La lista di tags viene filtrata da valori duplicati e
     * tutti i valori al suo interno vengono resi minuscoli. La data di creazione è aggiunta in automatico
     * ATTENZIONE: LA PASSWORD VA CRIPTATA ESTERNAMENTE!
     * @param username nome del nuovo utente
     * @param password password dell'utente hashata
     * @param tags_list lista di tags
     */
    public WinSomeUser(String username, String password, List<String> tags_list) {
        this.username = username;
        this.password = password;

        for(String tag : tags_list) {
            this.tags_list.add(tag.toLowerCase());
        }

        this.creationDate = System.currentTimeMillis();
    }

    /**
     * Fornisce l'username dell'utente
     * @return l'username dell'utente attuale
     */
    public String getUsername() {
        return username;
    }

    /**
     * Fornisce la password salvata dell'utente
     * @return password salvata dell'utente
     */
    public String getSavedPassword() {
        return password;
    }

    /**
     * Fornisce la data di creazione di questo account
     * @return data di creazione dell'account in tempo UNIX
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Fornisce la lista di tags con il quale l'utente si è registrato
     * @return lista di tags dell'utente
     */
    public Set<String> getTags_list() {
        return tags_list;
    }
}
