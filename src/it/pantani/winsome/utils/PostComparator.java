/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import it.pantani.winsome.entities.WinSomePost;

import java.util.Comparator;

public class PostComparator implements Comparator<WinSomePost> {
    @Override
    public int compare(WinSomePost o1, WinSomePost o2) {
        return Long.compare(o1.getDateSent(), o2.getDateSent());
    }
}
