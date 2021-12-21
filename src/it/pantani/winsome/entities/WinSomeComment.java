/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.entities;

public class WinSomeComment {
    private final String author;
    private final String content;
    private final long dateSent;

    public WinSomeComment(String author, String content) {
        this.author = author;
        this.content = content;
        this.dateSent = System.currentTimeMillis();
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public long getDateSent() {
        return dateSent;
    }
}
