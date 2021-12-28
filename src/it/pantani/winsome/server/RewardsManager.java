/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.WinSomePost;
import it.pantani.winsome.server.entities.WinSomeVote;
import it.pantani.winsome.other.ConfigManager;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce calcolo, assegnazione e l'invio delle notifiche dei premi in valuta agli autori e curatori di post.
 * Si occupa di aprire una connessione multicast su uno specifico indirizzo e porta in modo che i client winsome
 * possano collegarsi ed ascoltare le notifiche in arrivo.
 */
public class RewardsManager implements Runnable {
    private final ConfigManager config;
    private final SocialManager social;

    private InetAddress multicast_address;
    private int multicast_port;

    private int rewards_check_timeout;
    public long last_rewards_check;

    public int decimal_places;

    private int percentage_reward_author;
    private int percentage_reward_curator;

    private String author_reward_reason;
    private String curator_reward_reason;

    private volatile boolean stop = false;

    /**
     * Questo costruttore si occupa di gestire anche eventuali errori di configurazione nel file.
     * @param config L'oggetto che contiene tutte le proprietà ricevute in input dal file di configurazione
     * @param social L'oggetto che contiene tutti i metodi per accedere agli utenti e post del social network
     *
     * @throws ConfigurationException Nel caso un parametro della configurazione fosse errato viene stampato un errore
     */
    public RewardsManager(ConfigManager config, SocialManager social) throws ConfigurationException {
        this.config = config;
        this.social = social;

        // controllo indirizzo multicast
        try {
            multicast_address = InetAddress.getByName(config.getPreference("multicast_address"));
        } catch(UnknownHostException e) {
            throw new ConfigurationException("valore 'multicast_address' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(!multicast_address.isMulticastAddress()) {
            throw new ConfigurationException("valore 'multicast_address' non valido (" + multicast_address.getHostAddress() + " non e' un indirizzo multicast)");
        }

        // controllo porta multicast
        try {
            multicast_port = Integer.parseInt(config.getPreference("multicast_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'multicast_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(multicast_port <= 0 || multicast_port >= 65535) {
            throw new ConfigurationException("valore 'multicast_port' non valido (" +multicast_port + " non e' una porta valida)");
        }

        // controllo timeout premi in valuta winsome
        try {
            rewards_check_timeout = Integer.parseInt(config.getPreference("rewards_check_timeout"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'rewards_check_timeout' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(rewards_check_timeout <= 0) {
            throw new ConfigurationException("valore 'rewards_check_timeout' non valido (" + rewards_check_timeout + " deve essere un numero maggiore di 0)");
        }

        // controllo numero di cifre decimali della valuta winsome
        try {
            decimal_places = Integer.parseInt(config.getPreference("currency_decimal_places"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'currency_decimal_places' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(decimal_places < 0 || decimal_places > 8) {
            throw new ConfigurationException("valore 'currency_decimal_places' non valido (" + decimal_places + " dovrebbe essere compreso tra 0 e 8)");
        }

        // controllo tempo di ultima verifica premi (questo valore non dovrebbe essere modificato)
        try {
            last_rewards_check = Long.parseLong(config.getPreference("last_rewards_check"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'last_rewards_check' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(last_rewards_check < 0) {
            throw new ConfigurationException("valore 'last_rewards_check' non valido (" + last_rewards_check + " non puo' essere negativo)");
        }

        // controllo percentuale autore e curatore del premio in valuta winsome
        try {
            percentage_reward_author = Integer.parseInt(config.getPreference("percentage_reward_author"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'percentage_reward_author' non valido (" + e.getLocalizedMessage() + ")");
        }
        try {
            percentage_reward_curator = Integer.parseInt(config.getPreference("percentage_reward_curator"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'percentage_reward_curator' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(percentage_reward_author + percentage_reward_curator != 100) {
            throw new ConfigurationException("valore 'percentage_reward_curator' non valido (la somma delle percentuali percentage_reward_author e percentage_reward_curator non raggiunge 100)");
        }

        // controllo causali delle transazioni relative ai premi dei post
        author_reward_reason = config.getPreference("author_reward_reason_transaction");
        if(author_reward_reason == null) {
            throw new ConfigurationException("valore 'author_reward_reason' non valido (non e' presente nel file di configurazione)");
        }
        curator_reward_reason = config.getPreference("curator_reward_reason_transaction");
        if(curator_reward_reason == null) {
            throw new ConfigurationException("valore 'curator_reward_reason' non valido (non e' presente nel file di configurazione)");
        }
    }

    /**
     * Inizia il calcolo, per ogni singolo post, dei premi all'autore e curatore. Il controllo dei voti e commenti è
     * effettuato solo su post e commenti che hanno una data di invio (dateSent) superiore (quindi inviati dopo) a
     * quella dell'ultimo controllo.
     */
    public void run() {
        try {
            DatagramSocket socketServer = new DatagramSocket(null);
            InetAddress ia = InetAddress.getLocalHost(); // ottengo l'indirizzo della macchina attuale dinamicamente (su windows, "localhost" oppure "127.0.0.1" non sempre funziona)
            InetSocketAddress address = new InetSocketAddress(ia, multicast_port);
            socketServer.setReuseAddress(true);
            socketServer.bind(address);

            byte[] buffer_array;
            DatagramPacket packet;

            ConcurrentHashMap<Integer, WinSomePost> postsList;
            // il controllo dei premi è eseguito all'infinito fino a quando il server non viene spento
            while(!stop) {
                postsList = social.getPostList(); // ottengo tutti i post
                if(postsList.size() != 0) {
                    double gain, total_gain = 0;
                    for(WinSomePost p : postsList.values()) { // per ogni post calcolo il guadagno
                        gain = calculateReward(p);
                        total_gain += gain;
                    }
                    if (total_gain != 0) { // se il guadagno di tutti i post non è 0, allora invio un messaggio a tutti i client
                        buffer_array = social.getFormattedCurrency(total_gain).getBytes(StandardCharsets.UTF_8);

                        // invio dinamicamente la lunghezza della stringa che il client riceverà
                        ByteBuffer temp_buffer = ByteBuffer.allocate(Integer.BYTES).putInt(buffer_array.length);
                        packet = new DatagramPacket(temp_buffer.array(), temp_buffer.limit(), multicast_address, multicast_port);
                        socketServer.send(packet);

                        // ora invio la stringa vera e propria contenente: numero di valuta winsome + nome valuta (singolare o plurale)
                        packet = new DatagramPacket(buffer_array, buffer_array.length, multicast_address, multicast_port);
                        socketServer.send(packet);
                        System.out.println("[RW]> Inviata notifica su " + multicast_address + ":" + multicast_port + " riguardo il premio di " + social.getFormattedCurrency(total_gain));
                    }
                    last_rewards_check = System.currentTimeMillis(); // aggiorno ultimo controllo
                }

                // aspetto rewards_check_timeout millisecondi prima di effettuare di nuovo il controllo
                try {
                    Thread.sleep(rewards_check_timeout);
                } catch(InterruptedException ignored) {}
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo che si occupa di calcolare il premio in valuta winsome di un singolo post
     * @param p il post di cui devo effettuare il calcolo
     * @return il premio in valuta winsome relativo a quel post che andrà spartito tra autore e curatori
     */
    private double calculateReward(WinSomePost p) {
        double gain;
        int numIteration = p.addIteration(); // ottengo l'iterazione del post a cui sono

        double first_log = 0;
        ConcurrentHashMap<String, WinSomeVote> votes_list = p.getVoteList(last_rewards_check);
        for(WinSomeVote v : votes_list.values()) {
            first_log += v.getVote();
        }
        if(first_log <= 0) first_log = 0; // parte "max" della formula
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

        if(gain != 0) updateBalance(p.getPostID(), gain, votes_list, users_commenting);
        return gain;
    }

    /**
     * Aggiorna i bilanci dei vari utenti che hanno partecipato al post
     * @param post_id id del post di abbiamo calcolato il guadagno
     * @param gain il guadagno del post da spartire tra autore e curatori
     * @param votes_list la lista dei voti del post (ricevuta invece di calcolata perché nel frattempo potrebbero essere cambiati
     * @param users_commenting la lista dei commenti del post (ricevuta invece di calcolata perché nel frattempo potrebbero essere cambiati
     */
    private void updateBalance(int post_id, double gain, ConcurrentHashMap<String, WinSomeVote> votes_list, ArrayList<String> users_commenting) {
        String updated_reason;

        // curatori
        Set<String> curators = new LinkedHashSet<>();
        updated_reason = curator_reward_reason.replace("{post}", String.valueOf(post_id));

        curators.addAll(votes_list.keySet());

        curators.addAll(users_commenting);

        if(curators.size() == 0) return;

        double gain_per_curator = (gain*(percentage_reward_curator*0.01))/curators.size();
        for(String c : curators) {
            social.getWalletByUsername(c).changeBalance(gain_per_curator, updated_reason);
        }

        // autore
        updated_reason = author_reward_reason.replace("{post}", String.valueOf(post_id));

        double gain_per_author = gain*(percentage_reward_author*0.01);
        social.getWalletByUsername(social.getPost(post_id).getAuthor()).changeBalance((float)gain_per_author, updated_reason);
    }

    /**
     * Metodo per bloccare l'esecuzione del calcolo dei reward
     */
    public void stopExecution() {
        stop = true;
    }

    /**
     * Permette di salvare il dato sull'ultimo controllo dei premi in memoria persistente. Si è preferito tenere
     * questo metodo nella classe RewardsManager invece che nel main o nel SocialManager per cercare di tenere più
     * separati possibili i compiti delle singole classi.
     */
    public void savePersistentData() {
        config.forceSavePreference("last_rewards_check", String.valueOf(last_rewards_check));
    }
}
