/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

/**
 * Classe che rappresenta la struttura di un comment di WinSome. Contiene l'autore del commento, il contenuto
 * e ovviamente la data di invio in tempo UNIX. E' un oggetto immutabile.
 */
public class WinSomeComment {
    private final String author;
    private final String content;
    private final long dateSent;

    /**
     * Questo costruttore inizializza un oggetto di tipo commento. La data viene impostata in automatico.
     * @param author l'autore del commento
     * @param content il contenuto del commento
     */
    public WinSomeComment(String author, String content) {
        this.author = author;
        this.content = content;
        this.dateSent = System.currentTimeMillis();
    }

    /**
     * Fornisce l'autore del commento
     * @return autore del commento
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Fornisce il contenuto del commento
     * @return contenuto del commento
     */
    public String getContent() {
        return content;
    }

    /**
     * Fornisce la data di invio del commento
     * @return data invio del commento in tempo UNIX
     */
    public long getDateSent() {
        return dateSent;
    }
}
