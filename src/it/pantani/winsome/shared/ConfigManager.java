/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.shared;

import java.io.*;
import java.util.Properties;

/**
 * Classe che si occupa di caricare il file di configurazione di server e client o di generarlo se non presente.
 */
public class ConfigManager {
    private static final String WINSOME_FOLDER_NAME = "data"; // nome della cartella dove salvare i file di configurazione
    private static final String DEFAULT_GETPREFERENCE = null; // valore di default del metodo getPreference
    private static final String CONFIG_COMMENT = "#########################################################################\n Questo è file di configurazione di WINSOME.\n I parametri qui sotto dopo il '=' sono modificabili.\n Puoi invece inserire commenti mettendo un '#' all'inizio della riga\n##########################################################################";

    private static final String DEFAULT_SERVER_FILE_PATH = "config_server.properties"; // nome del config del server
    private static final String DEFAULT_CLIENT_FILE_PATH = "config_client.properties"; // nome del config del client

    private final Properties prop;
    private final String file_path;

    /**
     * Costruttore che inizializza un oggetto di tipo ConfigManager. Se il file non esiste, lo genera e imposta delle
     * variabili ai loro valori di default. Altrimenti carica dal file le variabili.
     * @throws IOException se si verifica un errore di lettura
     */
    public ConfigManager(boolean isServer) throws IOException {
        if(isServer) file_path = DEFAULT_SERVER_FILE_PATH; else file_path = DEFAULT_CLIENT_FILE_PATH;
        File f = new File(System.getProperty("user.dir"), WINSOME_FOLDER_NAME+"/"+file_path);

        if(!f.exists()) {
            boolean ignored = new File(WINSOME_FOLDER_NAME).mkdirs(); // altrimenti genera un warning
            OutputStream out = new FileOutputStream(WINSOME_FOLDER_NAME+"/"+file_path);
            prop = new Properties();
            if(isServer) {
                prop.setProperty("server_port", "6789"); // porta del server
                prop.setProperty("rmi_server_port", "1099"); // porta RMI del server
                prop.setProperty("rmi_server_registry_name", "winsome-server"); // nome registro RMI del server
                prop.setProperty("rmi_callback_client_port", "1100"); // porta callback RMI del client
                prop.setProperty("rmi_callback_client_registry_name", "winsome-server-callback"); // nome registro callback RMI del client
                prop.setProperty("post_max_title_length", "20"); // lunghezza massima del titolo del post
                prop.setProperty("post_max_content_length", "500"); // lunghezza massima del corpo del post
                prop.setProperty("currency_name_singular", "wincoin"); // nome della valuta singolare
                prop.setProperty("currency_name_plural", "wincoins"); // nome della valuta plurale
                prop.setProperty("currency_decimal_places", "2"); // cifre decimali
                prop.setProperty("multicast_address", "224.0.0.1"); // indirizzo multicast
                prop.setProperty("multicast_port", "6788"); // porta multicast
                prop.setProperty("autosave_interval", "60000"); // intervallo di salvataggio automatico dei dati di persistenza
                prop.setProperty("default_reason_transaction", "SYSTEM"); // causale di default di una transazione
                prop.setProperty("author_reward_reason_transaction", "Author reward for post #{post}"); // causale di una transazione relativa ad un premio (per l'autore del post winsome)
                prop.setProperty("curator_reward_reason_transaction", "Curator reward for post #{post}");  // causale di una transazione relativa ad un premio (per un curatore di un post winsome)
                prop.setProperty("percentage_reward_author", "70"); // percentuale del premio di un post dell'autore
                prop.setProperty("percentage_reward_curator", "30"); // percentuale del premio di un post del curatore
                prop.setProperty("rewards_check_timeout", "15000"); // tempo che il RewardsManager dovrà attendere ogni volta che fa un controllo prima di farne un altro
                prop.setProperty("last_rewards_check", "0"); // [da non modificare] data UNIX dell'ultimo controllo del RewardsManager
                prop.setProperty("last_post_id", "0"); // [da non modificare] contatore dell'ultimo post di WinSome
            } else {
                prop.setProperty("server_address", "localhost"); // indirizzo del server
                prop.setProperty("server_port", "6789"); // porta del server
                prop.setProperty("server_rmi_port", "1099"); // porta RMI del server
                prop.setProperty("client_rmi_callback_port", "1100"); // porta callback RMI del client
                prop.setProperty("server_rmi_registry_name", "winsome-server"); // nome registro RMI del server
                prop.setProperty("server_rmi_callback_registry_name", "winsome-server-callback"); // nome registro callback RMI
                prop.setProperty("multicast_server_address", "224.0.0.1"); // indirizzo multicast per le notifiche
                prop.setProperty("multicast_server_port", "6788"); // porta del multicast per le notifiche
            }
            prop.store(out, CONFIG_COMMENT);
            out.close();
        } else {
            InputStream in = new FileInputStream(WINSOME_FOLDER_NAME+"/"+file_path);
            prop = new Properties();
            prop.load(in);
            in.close();
        }
    }

    /**
     * Permette di ottenere una determinata preferenza dal file di configurazione precedentemente caricato. Ovviamente
     * il caricamento viene fatto solamente una volta (all'inizializzazione)
     * @param preference_name nome della preferenza
     * @return il valore della preferenza ottenuta dal config, null se non esiste o è vuota
     */
    public String getPreference(String preference_name) {
        if(prop == null) return null;
        return prop.getProperty(preference_name, DEFAULT_GETPREFERENCE);
    }

    /**
     * Permette di salvare una preferenza immediatamente nel file di configurazione.
     * @param preference_name nome della preferenza
     * @param value valore della preferenza
     */
    public void forceSavePreference(String preference_name, String value) {
        OutputStream out;
        try {
            out = new FileOutputStream(WINSOME_FOLDER_NAME+"/"+file_path);
        } catch(FileNotFoundException e) {
            System.err.println("[!] Impossibile salvare preferenza '" + preference_name + "' (valore: '" + value + "') nel file di configurazione. Motivo: " + e.getLocalizedMessage());
            return;
        }
        prop.setProperty(preference_name, value);
        try {
            prop.store(out, CONFIG_COMMENT);
            out.close();
        } catch(IOException e) {
            System.err.println("[!] Impossibile salvare preferenza '" + preference_name + "' (valore: '" + value + "') nel file di configurazione. Motivo: " + e.getLocalizedMessage());
        }
    }
}