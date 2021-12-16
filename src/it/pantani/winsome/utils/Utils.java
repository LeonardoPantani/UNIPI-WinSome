/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Utils {
    public static void send(PrintWriter out, String send) {
        int bytes = send.getBytes().length;
        out.println(bytes);
        out.println(send);
    }

    public static String receive(BufferedReader in) {
        StringBuilder everything = new StringBuilder();
        try {
            String dato = in.readLine();
            int l;
            try {
                l = Integer.parseInt(dato);
            } catch(NumberFormatException e) {
                return dato;
            }
            int i = -2;
            while (i < l) {
                everything.append((char)in.read());
                i++;
            }
        } catch(IOException ignored) {}
        return everything.substring(0, everything.length()-2);
    }
}
