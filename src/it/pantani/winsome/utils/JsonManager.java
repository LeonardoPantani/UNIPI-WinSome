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
import it.pantani.winsome.entities.WinSomeUser;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class JsonManager {
    private static final String user_data_path = "src/user_data.json"; // path to json file
    private static final String followers_data_path = "src/followers_data.json"; // path to json file
    private static final String following_data_path = "src/following_data.json"; // path to json file

    private final Gson gson;
    private JsonReader reader;

    public JsonManager() {
        gson = new Gson();
        if(createFile(user_data_path) != 0) System.err.println("[!] Errore creazione file dati utente");
        if(createFile(followers_data_path) != 0) System.err.println("[!] Errore creazione file relazione (followers)");
        if(createFile(following_data_path) != 0) System.err.println("[!] Errore creazione file relazione (following)");
    }

    public void loadAll(SocialManager s) {
        loadUserData(s);
        loadRelationsData(s);
    }

    public void loadUserData(SocialManager s) {
        String input = getFromFile(user_data_path);
        if(input == null) return;

        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentHashMap<String, WinSomeUser>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati utenti vuoto.");
            return;
        }
        s.setUserList(gson.fromJson(input, listaDelMioOggettoClasse)); // importazione nel social
    }

    public void loadRelationsData(SocialManager s) {
        // FOLLOWERS
        String input = getFromFile(followers_data_path);
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
        input = getFromFile(following_data_path);
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
        saveInFile(user_data_path, s.getUserList());

        saveInFile(followers_data_path, s.getFollowersList());
        saveInFile(following_data_path, s.getFollowingList());
    }

    public void saveInFile(String path, Object structure) throws IOException {
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
