/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Classe che si occupa di generare numeri pseudocasuali utilizzando un servizio esterno.
 */
public abstract class RandomGenerator {
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
}
