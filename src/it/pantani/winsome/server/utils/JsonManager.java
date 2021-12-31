/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import it.pantani.winsome.server.SocialManager;
import it.pantani.winsome.server.entities.WinSomePost;
import it.pantani.winsome.server.entities.WinSomeUser;
import it.pantani.winsome.server.entities.WinSomeWallet;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class JsonManager {
    private static final String WINSOME_FOLDER_NAME = "data"; // nome cartella

    // percorsi ai file json
    private static final String USER_DATA_PATH = "user_data.json";
    private static final String FOLLOWERS_DATA_PATH = "followers_data.json";
    private static final String FOLLOWING_DATA_PATH = "following_data.json";
    private static final String POSTS_DATA_PATH = "posts_data.json";
    private static final String WALLETS_DATA_PATH = "wallets_data.json";

    private final Gson gson;
    private JsonReader reader;

    /**
     * Costruttore che inizializza l'oggetto Gson (che permette di salvare oggetti e recuperarli da un file json). Inoltre,
     * crea i file che salvano le varie strutture dati per garantire persistenza al server. I file vengono creati
     * la prima volta nella cartella e con il nome specificato nelle costanti sopra, se non esistono già.
     */
    public JsonManager() {
        gson = new Gson();
        if(!createFile(WINSOME_FOLDER_NAME+"/"+ USER_DATA_PATH)) System.err.println("[!] Errore creazione file dati utente");
        if(!createFile(WINSOME_FOLDER_NAME+"/"+ WALLETS_DATA_PATH)) System.err.println("[!] Errore creazione file relazione (wallets)");
        if(!createFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH)) System.err.println("[!] Errore creazione file relazione (followers)");
        if(!createFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH)) System.err.println("[!] Errore creazione file relazione (following)");
        if(!createFile(WINSOME_FOLDER_NAME+"/"+ POSTS_DATA_PATH)) System.err.println("[!] Errore creazione file relazione (posts)");
    }

    /**
     * Utile per non dover chiamare tutti le funzioni una alla volta.
     * @param s l'oggetto social
     */
    public void loadAll(SocialManager s) {
        loadUserData(s);
        loadWalletsData(s);
        loadPostData(s);
        loadRelationsData(s);
    }

    /**
     * Carica i dati utente
     * @param s l'oggetto social
     */
    public void loadUserData(SocialManager s) {
        String input = getFromFile(WINSOME_FOLDER_NAME+"/"+ USER_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, WinSomeUser>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati utenti vuoto.");
            return;
        }
        s.setUserList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    /**
     * Carica i dati dei post
     * @param s l'oggetto social
     */
    public void loadPostData(SocialManager s) {
        String input = getFromFile(WINSOME_FOLDER_NAME+"/"+ POSTS_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<Integer, WinSomePost>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati post vuoto.");
            return;
        }
        s.setPostList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    /**
     * Carica i dati dei portafogli
     * @param s l'oggetto social
     */
    public void loadWalletsData(SocialManager s) {
        String input = getFromFile(WINSOME_FOLDER_NAME+"/"+ WALLETS_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, WinSomeWallet>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati wallet vuoto.");
            return;
        }
        s.setWalletList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    /**
     * Carica i dati dei followers e following
     * @param s l'oggetto social
     */
    public void loadRelationsData(SocialManager s) {
        // FOLLOWERS
        String input = getFromFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH);
        if(input != null) {
            if(input.equals("")) {
                System.out.println("> File dati relazioni (followers) vuoto.");
            } else {
                // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
                Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, ArrayList<String>>>() {}.getType();
                s.setFollowersList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
            }
        }

        // FOLLOWING
        input = getFromFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH);
        if(input != null) {
            if(input.equals("")) {
                System.out.println("> File dati relazioni (following) vuoto.");
            } else {
                // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
                Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, ArrayList<String>>>() {}.getType();
                s.setFollowingList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
            }
        }
    }

    /**
     * Salva tutti i file per garantire la persistenza del server.
     * @param s l'oggetto social
     * @throws IOException in caso di errori con il salvataggio dei dati
     */
    public void saveAll(SocialManager s) throws IOException {
        saveInFile(WINSOME_FOLDER_NAME+"/"+ USER_DATA_PATH, s.getUserList());
        saveInFile(WINSOME_FOLDER_NAME+"/"+ WALLETS_DATA_PATH, s.getWalletList());

        saveInFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH, s.getFollowersList());
        saveInFile(WINSOME_FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH, s.getFollowingList());

        saveInFile(WINSOME_FOLDER_NAME+"/"+ POSTS_DATA_PATH, s.getPostList());
    }

    /**
     * Salva in un file un determinato oggetto, che viene convertito in json.
     * @param path il percorso del file in cui salvare l'oggetto
     * @param structure l'oggetto da salvare
     * @throws IOException in caso di errori con il salvataggio dei dati
     */
    private void saveInFile(String path, Object structure) throws IOException {
        String toSave = gson.toJson(structure);

        // --- scrivo dati sul canale (file) - NIO
        WritableByteChannel dest;
        try {
            dest = Channels.newChannel(new FileOutputStream(path));
        } catch(Exception e) {
            System.err.println("[!] Percorso '" + path + "' non valido per il file.");
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(toSave.getBytes().length);
        buffer.put(toSave.getBytes());
        buffer.flip();
        dest.write(buffer);
        dest.close();
    }

    /**
     * Legge un file dinamicamente
     * @param path il percorso del file da leggere
     * @return il contenuto del file
     */
    private String getFromFile(String path) {
        // leggo dal file
        ReadableByteChannel src;

        try {
            src = Channels.newChannel(new FileInputStream(path));
        } catch(Exception e) {
            System.err.println("[!] Percorso '" + path + "' non valido per il file.");
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        String input = ""; // stringa che contiene il file
        buffer.clear();
        try {
            while(src.read(buffer) >= 0 || buffer.position() != 0) {
                // preparo a leggere i byte che sono stati inseriti nel buffer
                buffer.flip();

                // aggiungo a stringa
                input = getJsonData(input, buffer);

                // pulisco il buffer
                buffer.compact();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        // quando arrivo alla fine, è possibile che non tutti i dati sono stati letti
        buffer.flip();
        while(buffer.hasRemaining()) {
            input = getJsonData(input, buffer);
        }

        try {
            src.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return input;
    }

    /**
     * Crea un file, se non esiste
     * @param path il percorso del nuovo file
     * @return vero se il file viene creato, falso altrimenti
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean createFile(String path) {
        boolean ignored = new File(WINSOME_FOLDER_NAME).mkdirs(); // genero la cartella, se non esiste già

        try {
            File f = new File(path);
            // se il file non esiste e non è stato creato dò errore
            return f.exists() || f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Visto che con NIO i file non sono letti tutti insieme, devo salvarmi man mano i byte su una stringa,
     * che, una volta finiti tutti i dati, comporrà il file sorgente.
     * @param extend la stringa da estendere ogni volta con i nuovi dati
     * @param buffer i nuovi byte (dati) con cui estendere la stringa extend
     * @return la stringa estesa con i nuovi dati
     */
    private String getJsonData(String extend, ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        sb.append(extend);
        while(buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }

        return sb.toString();
    }
}
