/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import it.pantani.winsome.ServerMain;
import it.pantani.winsome.entities.WinSomeUser;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

public class JsonManager {
    private static String user_data_path = "src/user_data.json"; // path to json file

    private Gson gson;
    private JsonReader reader;

    public JsonManager() {
        gson = new Gson();
        try {
            reader = new JsonReader(new FileReader(user_data_path));
        } catch (FileNotFoundException e) {
            System.out.println("> File dati utente non trovato. Lo genero e inizializzo.");
            try {
                initialization();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void load() {
        // leggo dal file
        ReadableByteChannel src;

        try {
            src = Channels.newChannel(new FileInputStream(user_data_path));
        } catch(Exception e) {
            System.err.println("[!] Percorso '" + user_data_path + "' non valido per il file.");
            return;
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

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // se non ci fosse questa riga, la JVM non sarebbe in grado di ricavare la struttura esatta degli oggetti serializzati
        Type listaDelMioOggettoClasse = new TypeToken<ConcurrentLinkedQueue<WinSomeUser>>() {}.getType();
        if(input.equals("")) {
            System.out.println("> File dati utenti vuoto.");
            return;
        }
        ServerMain.listaUtenti = gson.fromJson(input, listaDelMioOggettoClasse);
    }

    private void initialization() throws IOException {
        String newData = "";

        // --- scrivo dati sul canale (file) - NIO
        WritableByteChannel dest;
        try {
            dest = Channels.newChannel(new FileOutputStream(user_data_path));
        } catch(Exception e) {
            System.err.println("[!] Percorso '" + user_data_path + "' non valido per il file.");
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(newData.getBytes().length);
        buffer.put(newData.getBytes());
        buffer.flip();
        dest.write(buffer);
        dest.close();
    }

    public void save() throws IOException {
        String toSave = gson.toJson(ServerMain.listaUtenti);

        // --- scrivo dati sul canale (file) - NIO
        WritableByteChannel dest;
        try {
            dest = Channels.newChannel(new FileOutputStream(user_data_path));
        } catch(Exception e) {
            System.err.println("[!] Percorso '" + user_data_path + "' non valido per il file.");
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(toSave.getBytes().length);
        buffer.put(toSave.getBytes());
        buffer.flip();
        dest.write(buffer);
        dest.close();
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
