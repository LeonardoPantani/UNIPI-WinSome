/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.other;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Classe utilizzata sia da client che server perché contiene utili funzioni che permettono il funzionamento di WinSome.
 */
public class Utils {
    public static final String SOCIAL_REGISTRATION_SUCCESS = "registration ok";
    public static final String SOCIAL_REGISTRATION_FAILED = "registration failed";

    public static final String SOCIAL_LOGIN_SUCCESS = "login ok";

    public static final String SOCIAL_LOGOUT_SUCCESS = "logout ok";

    // indirizzo utilizzato per il recupero di valori casuali
    public static final String web_address = "https://www.random.org/decimal-fractions/?num=1&dec=20&col=1&format=plain&rnd=new";

    /**
     * Contatta il servizio esterno per ottenere un valore casuale e lo restituisce come numero double.
     * @return il valore double ottenuto da remoto
     */
    public static double generateRandomValue() {
        try {
            URL url = new URL(web_address);
            URLConnection url_conn = url.openConnection();

            // apro uno stream
            BufferedReader buf_read = new BufferedReader(new InputStreamReader(url_conn.getInputStream()));
            String body = buf_read.readLine();
            buf_read.close();

            return Double.parseDouble(body);
        } catch(IOException | NumberFormatException e) {
            System.err.println("[!] Errore durante generazione di valore casuale. Motivo: " + e.getLocalizedMessage());
        }
        return 0;
    }

    /**
     * "Hack" che permette di inviare, dato uno stream, una stringa anche se contiene nuove righe.
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
     * Permette di ricevere una stringa anche se contiene varie righe.
     * @param in lo stream da cui ricevere la stringa
     * @return la stringa ricevuta
     * @throws IOException se la lunghezza della stringa inviata non è valida
     */
    public static String receive(BufferedReader in) throws IOException {
        StringBuilder everything = new StringBuilder();
        // ottengo la lunghezza della stringa che riceverò
        String dato = in.readLine();
        if(dato == null) throw new IOException(); // per linux

        int l;
        try {
            l = Integer.parseInt(dato);
        } catch(NumberFormatException e) {
            return dato;
        }

        int i = 0;
        while(i < l) {
            everything.append((char)in.read());
            i++;
        }
        return everything.toString();
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