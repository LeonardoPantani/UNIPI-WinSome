/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Classe utilizzata sia da client che server perché contiene utili funzioni che permettono il funzionamento di WinSome.
 */
public abstract class Utils {
    public static final String SOCIAL_REGISTRATION_SUCCESS = "registration ok";
    public static final String SOCIAL_REGISTRATION_FAILED = "registration failed";

    public static final String SOCIAL_LOGIN_SUCCESS = "login ok";
    public static final String SOCIAL_LOGOUT_SUCCESS = "logout ok";

    /**
     * Permette di inviare una stringa su uno stream.
     * @param out lo stream su cui inviare la stringa
     * @param send la stringa da inviare
     */
    public static void send(PrintWriter out, String send) {
        int bytes = send.getBytes().length;
        out.println(bytes); // invio prima la lunghezza della stringa...
        out.print(send); // ... e poi la invio
        out.flush();
    }

    /**
     * Permette di ricevere una stringa da uno stream.
     * @param in lo stream da cui ricevere la stringa
     * @return la stringa ricevuta
     * @throws IOException se la lunghezza della stringa inviata non è valida
     */
    public static String receive(BufferedReader in) throws IOException {
        StringBuilder everything = new StringBuilder();

        // ottengo la lunghezza della stringa che riceverò
        String dato = in.readLine();
        if(dato == null) throw new IOException(); // per unix
        int l;
        try {
            l = Integer.parseInt(dato);
        } catch(NumberFormatException e) {
            return dato;
        }

        // leggo il dato in in base al numero di bytes ricevuto come primo messaggio dalla in.readLine()
        int i = 0;
        while(i < l) {
            everything.append((char)in.read());
            i++;
        }
        return everything.toString();
    }

    /**
     * Permette di leggere una stringa da console. A differenza del readLine() normale, gestisce il caso della
     * NoSuchElementException che si verifica se il programma termina mentre si è in attesa di leggere un input.
     * @param reader istanza del lettore
     * @return la stringa letta
     */
    public static String readFromConsole(Scanner reader) {
        String input = "";

        try {
            input = reader.nextLine();
        } catch(NoSuchElementException ignored) {} // per evitare errore se si preme CTRL+C su Windows

        return input;
    }

    /**
     * Restituisce una stringa contenente la data formattata partendo dai millisecondi UNIX
     * @param date il tempo fornito in formato UNIX
     * @return stringa rappresentante la data fornita in formato UNIX
     */
    public static String getFormattedDate(long date) {
        Instant instant = Instant.ofEpochSecond(date/1000);
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                                        .withLocale(Locale.ITALY)
                                                        .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    /**
     * Approssima un numero al valore più vicino con determinate cifre dopo la virgola (precisione)
     * @param input il numero da approssimare
     * @param precision il numero di cifre dopo la virgola che si vogliono ottenere
     * @return il numero approssimato
     */
    public static double roundC(double input, int precision) {
        double multiplier = Math.pow(10, precision);
        return Math.round(input * multiplier) / multiplier;
    }
}