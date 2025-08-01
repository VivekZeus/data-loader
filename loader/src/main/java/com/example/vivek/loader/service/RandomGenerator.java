package com.example.vivek.loader.service;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RandomGenerator {

    private  final String[] FIRST_NAMES = {
            "Alice", "Bob", "Charlie", "Diana", "Ethan", "Fiona", "George", "Hannah"
    };

    private  final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis"
    };

    public   String generateRandomName() {
        Random rand = new Random();
        String firstName = FIRST_NAMES[rand.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[rand.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    public  int generateSixDigitNumber() {
        return 100000 + new Random().nextInt(900000); // 100000 to 999999
    }

    public  int generateBetween20And100() {
        return 20 + new Random().nextInt(81); // 20 to 100
    }

    public String generateApiKey(){
        return "X_API_KEY_"+ NanoIdUtils.randomNanoId();
    }
}
