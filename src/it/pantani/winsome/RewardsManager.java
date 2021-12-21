/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.utils.ConfigManager;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class RewardsManager implements Runnable {
    private final ConfigManager config;
    private InetAddress multicast_address;
    private int multicast_port;
    private int rewards_check_timeout;

    private volatile boolean stop = false;

    public RewardsManager(ConfigManager config) {
        this.config = config;

        String addr = config.getPreference("multicast_address");
        try {
            multicast_address = InetAddress.getByName(addr);
        } catch(UnknownHostException e) {
            System.err.println("[!] Errore, configurazione multicast_address non valida, motivo: " + e.getLocalizedMessage());
            return;
        }
        try {
            multicast_port = Integer.parseInt(config.getPreference("multicast_port"));
        } catch(NumberFormatException e) {
            System.err.println("[!] Errore, numero multicast_port non valido: " + e.getLocalizedMessage());
            return;
        }
        try {
            rewards_check_timeout = Integer.parseInt(config.getPreference("rewards_check_timeout"));
        } catch(NumberFormatException e) {
            System.err.println("[!] Errore, numero rewards_check_timeout non valido: " + e.getLocalizedMessage());
        }
    }
    @Override
    public void run() {
        try {
            DatagramSocket socketServer = new DatagramSocket(null);
            InetAddress ia = InetAddress.getLocalHost(); // ottengo l'indirizzo della macchina attuale dinamicamente (su windows, "localhost" oppure "127.0.0.1" non sempre funziona)
            InetSocketAddress indirizzo = new InetSocketAddress(ia, multicast_port);
            socketServer.setReuseAddress(true);
            socketServer.bind(indirizzo);

            byte[] array_buffer;
            DatagramPacket pacchetto;

            while(!stop) {
                array_buffer = "ciao".getBytes(StandardCharsets.UTF_8); // TODO invio periodico notifiche delle ricompense
                pacchetto = new DatagramPacket(array_buffer, array_buffer.length, multicast_address, multicast_port);

                socketServer.send(pacchetto);
                try {
                    Thread.sleep(rewards_check_timeout);
                } catch(InterruptedException ignored) {}
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void stopExecution() {
        stop = true;
    }
}
