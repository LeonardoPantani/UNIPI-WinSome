/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.client;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Gestisce la stampa della notifica di aggiornamento dei portafogli degli utenti WinSome. Visto
 * che la notifica è uguale per tutti, non è possibile capire se anche il portafoglio dell'utente
 * attualmente collegato è stato aggiornato o meno.
 */
public class WalletUpdateManager implements Runnable {
    // non una variabile locale in modo da poter chiudere il socket forzatamente all'arresto del server
    private MulticastSocket multicast_socket;

    private InetAddress multicast_address;
    private int multicast_port;

    private volatile boolean stop = false;

    /**
     * Questo costruttore si occupa di verificare che l'indirizzo multicast sia corretto e di preparare
     * il socket multicast.
     * @param multicast_address indirizzo multicast
     * @param multicast_port porta multicast
     */
    public WalletUpdateManager(String multicast_address, int multicast_port) throws ConfigurationException {
        try {
            this.multicast_address = InetAddress.getByName(multicast_address);
        } catch(UnknownHostException e) {
            System.err.println("[!] Errore, parametro multicast_address non valido, motivo: " + e.getLocalizedMessage());
        }
        if(!this.multicast_address.isMulticastAddress()) {
            throw new ConfigurationException("valore 'multicast_address' non valido (" + this.multicast_address.getHostAddress() + " non e' un indirizzo multicast)");
        }

        this.multicast_port = multicast_port;
        if(multicast_port <= 0 || multicast_port >= 65535) {
            throw new ConfigurationException("valore 'multicast_port' non valido (" +multicast_port + " non e' una porta valida)");
        }
    }

    /**
     * Inizializzo il multicast_socket, entro nel gruppo multicast e mi metto in ascolto dei pacchetti fino a quando
     * stop non è vera (viene impostata a tale valore solo da stopExecution() chiamata nel main alla chiusura del server).
     * Alla ricezione di ogni pacchetto stampo un messaggio nel client e ricomincio ad ascoltare.
     */
    public void run() {
        try {
            multicast_socket = new MulticastSocket(multicast_port);
            multicast_socket.joinGroup(multicast_address);

            byte[] buffer_array;
            DatagramPacket packet;

            // ascolto pacchetti in arrivo
            while(!stop) {
                try {
                    ByteBuffer temp_buffer = ByteBuffer.allocate(Integer.BYTES); // conterrà soltanto la dimensione della stringa ricevuta
                    packet = new DatagramPacket(temp_buffer.array(), temp_buffer.limit());
                    multicast_socket.receive(packet); // questo primo pacchetto contiene solo la dimensione della stringa successiva
                    int string_dimension = ByteBuffer.wrap(packet.getData()).getInt(); // ottengo la dimensione

                    buffer_array = new byte[string_dimension]; // alloco dinamicamente
                    packet = new DatagramPacket(buffer_array, string_dimension);
                    multicast_socket.receive(packet); // ricevo il secondo pacchetto che contiene i dati utili
                    String total_gain = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("> Il server ha distribuito un totale di " + total_gain + " come premi per vari post, puoi vedere il tuo portafoglio con il comando 'wallet'.");
                } catch(IOException ignored) { }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Termina l'attesa di pacchetti da parte del thread bloccato in un loop infinito.
     */
    public void stopExecution() {
        stop = true;
        multicast_socket.close();
    }
}
