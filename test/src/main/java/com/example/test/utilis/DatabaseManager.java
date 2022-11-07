package com.example.test.utilis;


import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

public class DatabaseManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public DatabaseManager() {
        mongoClient = new MongoClient("localhost", 27017);
        database = mongoClient.getDatabase("open5gs");
        collection = database.getCollection("subscribers");
    }

    public DatabaseManager(String host, int port) {
        mongoClient = new MongoClient(host, port);
        database = mongoClient.getDatabase("open5gs");
        collection = database.getCollection("subscribers");
    }

    public boolean checkValue(String input) {
        Document nameDoc = collection.find(Filters.lte("imsi",input)).first();
        
        if( nameDoc == null || nameDoc.isEmpty())
            return false;
        else
            return true;
    }
    
}
