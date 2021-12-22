/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomePost;
import it.pantani.winsome.entities.WinSomeVote;
import it.pantani.winsome.utils.ConfigManager;
import it.pantani.winsome.utils.Utils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RewardsManager implements Runnable {
    private final ConfigManager config;
    private final SocialManager social;

    private InetAddress multicast_address;
    private int multicast_port;
    private int rewards_check_timeout;

    public long last_rewards_check;

    private int percentage_reward_author;
    private int percentage_reward_curator;

    private volatile boolean stop = false;

    public RewardsManager(ConfigManager config, SocialManager social) {
        this.config = config;
        this.social = social;

        try {
            multicast_address = InetAddress.getByName(config.getPreference("multicast_address"));
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
        try {
            last_rewards_check = Long.parseLong(config.getPreference("last_rewards_check"));
        } catch(NumberFormatException e) {
            System.err.println("[!] Errore, numero last_rewards_check non valido: " + e.getLocalizedMessage());
        }
        try {
            percentage_reward_author = Integer.parseInt(config.getPreference("percentage_reward_author"));
        } catch(NumberFormatException e) {
            System.err.println("[!] Errore, numero last_rewards_check non valido: " + e.getLocalizedMessage());
        }
        try {
            percentage_reward_curator = Integer.parseInt(config.getPreference("percentage_reward_curator"));
        } catch(NumberFormatException e) {
            System.err.println("[!] Errore, numero last_rewards_check non valido: " + e.getLocalizedMessage());
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

            ConcurrentHashMap<Integer, WinSomePost> postsList;
            while(!stop) {
                postsList = social.getPostList();
                if(postsList != null) {
                    double gain;
                    for(WinSomePost p : postsList.values()) {
                        gain = calculateReward(p);
                        System.out.println("[Calcolo Reward]> Post #" + p.getPostID() + " ha ricevuto: " + gain);
                        array_buffer = (p.getPostID()+"|"+gain).getBytes(StandardCharsets.UTF_8);
                        pacchetto = new DatagramPacket(array_buffer, array_buffer.length, multicast_address, multicast_port);

                        socketServer.send(pacchetto);
                    }
                    // finito di inviare i messaggi relativi ad ogni post
                }
                // dormo per rewards_check_timeout millisecondi
                try {
                    Thread.sleep(rewards_check_timeout);
                } catch(InterruptedException ignored) {}
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateReward(WinSomePost p) {
        double gain;

        int numIteration = p.addIteration();

        double first_log = 0;
        ConcurrentHashMap<String, WinSomeVote> votes_list = p.getVoteList(last_rewards_check);
        for(WinSomeVote v : votes_list.values()) {
            first_log += v.getVote();
        }
        first_log++;
        first_log = Math.log(first_log);
        // prima parte pronta

        double second_log = 0;
        ArrayList<String> users_commenting = p.getUsersCommenting(last_rewards_check);
        for(String user : users_commenting) {
            int total_comments_by_user = p.findCommentsByUser(user).size();
            second_log += (2/(1+Math.pow(Math.E, -(total_comments_by_user-1))));
        }
        second_log++;
        second_log = Math.log(second_log);
        // seconda parte pronta

        gain = (first_log + second_log)/numIteration;
        gain = Utils.roundC(gain, 1); // approssimazione per eccesso a 1 cifra decimale

        updateBalance(p.getPostID(), gain, votes_list, users_commenting);

        last_rewards_check = System.currentTimeMillis(); // aggiorno ultimo controllo

        return gain;
    }



    private void updateBalance(int post_id, double gain, ConcurrentHashMap<String, WinSomeVote> votes_list, ArrayList<String> users_commenting) {
        // curatori
        Set<String> curators = new LinkedHashSet<>();

        curators.addAll(votes_list.keySet());

        curators.addAll(users_commenting);

        if(curators.size() == 0) return;

        double gain_per_curator = (gain*(percentage_reward_curator*0.01))/curators.size();
        for(String c : curators) {
            social.getWalletByUsername(c).changeBalance((float)gain_per_curator);
        }

        // autore
        double gain_per_author = gain*(percentage_reward_author*0.01);
        social.getWalletByUsername(social.getPost(post_id).getAuthor()).changeBalance((float)gain_per_author);
    }

    public void stopExecution() {
        stop = true;
    }
}
