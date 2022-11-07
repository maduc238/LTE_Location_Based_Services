package com.example.test;

import java.security.KeyStore.PrivateKeyEntry;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

@Component
public class DatabaseManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private boolean auth;

    public DatabaseManager() {
        System.out.println("This is DatabaseManager constructor!");
        mongoClient = new MongoClient("localhost", 27017);
        database = mongoClient.getDatabase("open5gs");
        for(String name : mongoClient.listDatabaseNames()) {
            System.out.println(name);

        }
    }

    public DatabaseManager(String host, int port) {
        mongoClient = new MongoClient(host, port);
    }

    
}
