/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.prefs.Preferences;

public class ConfigManager {
    private static final String FILE_PATH = "src/config.txt"; // path to configuration file
    private static final String DEFAULT_GETPREFERENCE = null; // default value for getPreference method

    private BufferedReader reader;
    private final Preferences prefs = Preferences.userRoot();

    public ConfigManager() {
        try {
            reader = new BufferedReader(new FileReader(FILE_PATH));
        } catch(FileNotFoundException e) {
            System.err.println("[!] File di configurazione '" + FILE_PATH + "' non trovato.");
            return;
        }

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if(line.charAt(0) == '#') {
                    continue;
                }
                String[] conf_line = line.split("=", 2);
                if(conf_line.length == 1) {
                    System.err.println("[!] Opzione di configurazione '" + conf_line[0] + "' malformata.");
                    continue;
                }
                if (conf_line[1].length() == 0) {
                    System.err.println("[!] Opzione di configurazione '" + conf_line[0] + "' con valore vuoto.");
                    break;
                }

                prefs.put(conf_line[0], conf_line[1]);
            }
        } catch(IOException e) {
            System.err.println("[!] Errore durante la lettura dal file di configurazione '" + FILE_PATH + "'.");
        }
    }

    public String getPreference(String preference_name) {
        return prefs.get(preference_name, DEFAULT_GETPREFERENCE);
    }

    public static void printStringArray(String[] a) { // temporary method used for debugging
        for (int i = 0; i < a.length; i++) {
            System.out.println(i + ") '" + a[i] + "' [LENGTH: " + a[i].length() + "]");
        }
    }
}
