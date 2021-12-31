/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.other.ConfigManager;
import it.pantani.winsome.server.utils.JsonManager;

import javax.naming.ConfigurationException;
import java.io.IOException;

/**
 * Classe che implementa il salvataggio dei dati persistenti in modo periodico. Per evitare che un arresto improvviso
 * faccia perdere tutti i dati al server, questo thread salva periodicamente i dati in memoria. Inoltre, al termine
 * del server (col comando "stopserver") questo metodo esegue un ultimo salvataggio prima di terminare.
 */
public class PeriodicSaveManager implements Runnable {
    private final ConfigManager config;
    private final JsonManager jsonmngr;
    private final SocialManager social;
    private final RewardsManager rewards;

    private volatile boolean stop;

    long autosave_interval;

    /**
     * Costruttore della classe PeriodicSaveManager. Riceve come parametri tutti i dati da salvare, più il file
     * di config che invece è usato per ottenere una preferenza chiamata "autosave_interval".
     * @param config il file di config da cui ottenere la preferenza "autosave_interval"
     * @param json da salvare
     * @param social da salvare
     * @param rewards da salvare
     * @throws ConfigurationException se la preferenza fornita non è valida
     */
    public PeriodicSaveManager(ConfigManager config, JsonManager json, SocialManager social, RewardsManager rewards) throws ConfigurationException {
        this.config = config;
        this.jsonmngr = json;
        this.social = social;
        this.rewards = rewards;
        stop = false;

        // validazione preferenze
        validateAndSavePreferences();
    }

    /**
     * Metodo eseguito all'attivazione del thread. Questo metodo cicla indefinitivamente per salvare, ad intervalli
     * di autosave_interval millisecondi, i dati di persistenza. Al termine del server, questo thread va terminato chiamando
     * stopExecution() e (eventualmente) il metodo thread.interrupt() se non si vuole aspettare la sleep.
     */
    public void run() {
        long inizio;

        while(!stop) {
            // la sleep è subito all'inizio del ciclo e non in fondo per non salvare i dati appena il thread viene avviato
            try {
                Thread.sleep(autosave_interval);
            } catch(InterruptedException ignored) { }

            try {
                inizio = System.currentTimeMillis();
                jsonmngr.saveAll(social);
                social.savePersistentData();
                rewards.savePersistentData();
                if(!stop) System.out.println("[SDM]> Salvataggio periodico dati completato, ha richiesto " + (System.currentTimeMillis() - inizio) + "ms."); // se il server è in fase di stop non c'è bisogno di questa stampa
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("> Periodic save manager chiuso.");
    }

    /**
     * Metodo per bloccare l'esecuzione del thread di salvataggio dati. Chiamare solo questo metodo fa attendere
     * che la sleep finisca prima di terminare il thread. Chiamare questo metodo insieme a thread.interrupt() permette
     * di terminare subito l'esecuzione del thread senza aspettare la sleep.
     * ATTENZIONE: non chiamare esclusivamente thread.interrupt() perchè questa operazione terminerebbe la sleep
     * (se attualmente il thread ci è bloccato) ma non il thread che comunque resterebbe in ciclo.
     */
    public void stopExecution() {
        stop = true;
    }

    /**
     * Verifica che le preferenze specificate nel file di configurazione siano corrette facendo vari controlli. Se
     * anche una sola opzione è errata allora lancia un'eccezione.
     * @throws ConfigurationException se una opzione della configurazione è errata
     */
    private void validateAndSavePreferences() throws ConfigurationException {
        // controllo che l'intervallo tra salvataggi si valido
        try {
            autosave_interval = Long.parseLong(config.getPreference("autosave_interval"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'autosave_interval' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(autosave_interval <= 1000) {
            throw new ConfigurationException("valore 'autosave_interval' non valido (deve essere maggiore di 1000 (ms))");
        }
    }
}
