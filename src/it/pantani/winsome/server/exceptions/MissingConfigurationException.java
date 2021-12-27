/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server.exceptions;

public class MissingConfigurationException extends Exception {
    public MissingConfigurationException(String name) {
        super("Configuration '" + name + "' does not have any value");
    }
}
