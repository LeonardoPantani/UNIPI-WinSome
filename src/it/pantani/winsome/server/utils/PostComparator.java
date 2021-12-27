/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.utils;

import it.pantani.winsome.server.entities.WinSomePost;

import java.util.Comparator;

/**
 * Semplice classe che implementa il metodo compare di due post, in modo da ordinarli per data di invio.
 */
public class PostComparator implements Comparator<WinSomePost> {
    @Override
    public int compare(WinSomePost o1, WinSomePost o2) {
        return Long.compare(o1.getDateSent(), o2.getDateSent());
    }
}
