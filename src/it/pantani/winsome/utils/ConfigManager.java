/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import it.pantani.winsome.RewardsManager;
import it.pantani.winsome.SocialManager;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String FOLDER_NAME = "data"; // name of folder
    private static final String FILE_PATH = "config.properties"; // path to configuration file
    private static final String DEFAULT_GETPREFERENCE = null; // default value for getPreference method
    private static final String CONFIG_COMMENT = "#########################################################################\n Questo è file di configurazione di WINSOME.\n I parametri qui sotto dopo il '=' sono modificabili.\n Puoi invece inserire commenti mettendo un '#' all'inizio della riga\n##########################################################################";

    private final Properties prop;

    public ConfigManager() throws IOException {
        File f = new File(System.getProperty("user.dir"), FOLDER_NAME+"/"+FILE_PATH);
        if(!f.exists()) {
            boolean ignored = new File(FOLDER_NAME).mkdirs(); // sennò genera un warning
            OutputStream out = new FileOutputStream(FOLDER_NAME+"/"+FILE_PATH);
            prop = new Properties();
            prop.setProperty("server_port", "6789");
            prop.setProperty("rmi_server_port", "1099");
            prop.setProperty("rmi_callback_client_port", "1100");
            prop.setProperty("post_max_title_length", "20");
            prop.setProperty("post_max_content_length", "500");
            prop.setProperty("currency_name_singular", "wincoin");
            prop.setProperty("currency_name_plural", "wincoins");
            prop.setProperty("multicast_address", "224.0.0.1");
            prop.setProperty("multicast_port", "6788");
            prop.setProperty("percentage_reward_author", "70"); // %
            prop.setProperty("percentage_reward_curator", "30"); // %
            prop.setProperty("rewards_check_timeout", "10000"); // ms
            prop.setProperty("last_rewards_check", "0"); // ms
            prop.setProperty("last_post_id", "0");
            prop.store(out, CONFIG_COMMENT);
            out.close();
        } else {
            InputStream in = new FileInputStream(FOLDER_NAME+"/"+FILE_PATH);
            prop = new Properties();
            prop.load(in);
            in.close();
        }
    }

    public String getPreference(String preference_name) {
        if(prop == null) return null;
        return prop.getProperty(preference_name, DEFAULT_GETPREFERENCE);
    }

    public void saveConfigData(SocialManager s, RewardsManager rm) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(FOLDER_NAME+"/"+FILE_PATH);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        if(out == null) {
            System.err.println("> Impossibile salvare dati persistenti del social in memoria.");
            return;
        }
        prop.setProperty("last_post_id", String.valueOf(s.last_post_id.intValue()));
        prop.setProperty("last_rewards_check", String.valueOf(rm.last_rewards_check));
        try {
            prop.store(out, CONFIG_COMMENT);
        } catch(IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}