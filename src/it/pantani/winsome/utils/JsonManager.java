/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import it.pantani.winsome.SocialManager;
import it.pantani.winsome.entities.WinSomePost;
import it.pantani.winsome.entities.WinSomeUser;
import it.pantani.winsome.entities.WinSomeWallet;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class JsonManager {
    private static final String FOLDER_NAME = "data"; // name of folder

    private static final String USER_DATA_PATH = "user_data.json"; // path to json file
    private static final String FOLLOWERS_DATA_PATH = "followers_data.json"; // path to json file
    private static final String FOLLOWING_DATA_PATH = "following_data.json"; // path to json file
    private static final String POSTS_DATA_PATH = "posts_data.json"; // path to json file
    private static final String WALLETS_DATA_PATH = "wallets_data.json"; // path to json file

    private final Gson gson;
    private JsonReader reader;

    public JsonManager() {
        gson = new Gson();
        if(createFile(FOLDER_NAME+"/"+ USER_DATA_PATH) != 0) System.err.println("[!] Errore creazione file dati utente");
        if(createFile(FOLDER_NAME+"/"+ WALLETS_DATA_PATH) != 0) System.err.println("[!] Errore creazione file relazione (wallets)");
        if(createFile(FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH) != 0) System.err.println("[!] Errore creazione file relazione (followers)");
        if(createFile(FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH) != 0) System.err.println("[!] Errore creazione file relazione (following)");
        if(createFile(FOLDER_NAME+"/"+ POSTS_DATA_PATH) != 0) System.err.println("[!] Errore creazione file relazione (following)");
    }

    public void loadAll(SocialManager s) {
        loadUserData(s);
        loadWalletsData(s);
        loadPostData(s);
        loadRelationsData(s);
    }

    public void loadUserData(SocialManager s) {
        String input = getFromFile(FOLDER_NAME+"/"+ USER_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, WinSomeUser>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati utenti vuoto.");
            return;
        }
        s.setUserList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    public void loadPostData(SocialManager s) {
        String input = getFromFile(FOLDER_NAME+"/"+ POSTS_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<Integer, WinSomePost>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati post vuoto.");
            return;
        }
        s.setPostList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    public void loadWalletsData(SocialManager s) {
        String input = getFromFile(FOLDER_NAME+"/"+ WALLETS_DATA_PATH);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, WinSomeWallet>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati wallet vuoto.");
            return;
        }
        s.setWalletList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    public void loadRelationsData(SocialManager s) {
        // FOLLOWERS
        String input = getFromFile(FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH);
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
        input = getFromFile(FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH);
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

    public void saveAll(SocialManager s) throws IOException {
        saveInFile(FOLDER_NAME+"/"+ USER_DATA_PATH, s.getUserList());
        saveInFile(FOLDER_NAME+"/"+ WALLETS_DATA_PATH, s.getWalletList());

        saveInFile(FOLDER_NAME+"/"+ FOLLOWERS_DATA_PATH, s.getFollowersList());
        saveInFile(FOLDER_NAME+"/"+ FOLLOWING_DATA_PATH, s.getFollowingList());

        saveInFile(FOLDER_NAME+"/"+ POSTS_DATA_PATH, s.getPostList());
    }

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

    private int createFile(String path) {
        boolean ignored = new File(FOLDER_NAME).mkdirs(); // sennò genera un warning

        try {
            File f = new File(path);
            if(!f.exists() && !f.createNewFile()) {
                return -1;
            }
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
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
