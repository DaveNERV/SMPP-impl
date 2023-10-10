package org.example.server;

public class Main {

    public static void main(String[] args) {

        SMSC smppServerSim = new SMSC(2777, "user", "123");
        smppServerSim.run();
    }
}
