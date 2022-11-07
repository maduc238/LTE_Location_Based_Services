package com.example.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ShServer {
    @Autowired
    private DatabaseManager database;

    @Autowired
    private Printor printor;

    @Autowired
    private StackCreator stackCreator;

    public ShServer(DatabaseManager database, Printor printor, StackCreator stackCreator) {
        System.out.println("This is ShServer constructor");
        this.database = database;

        this.printor = printor;

        this.stackCreator = stackCreator;
    }

    public ShServer() {
        System.out.println("This is ShServer constructor");
    }
}
