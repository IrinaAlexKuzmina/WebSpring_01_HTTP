package ru.netology;

import static ru.netology.utils.Constants.PORT;

public class Main {
    public static void main(String[] args) {
      Server server = new Server(PORT);
        server.runPoolOfSockets();
    }
}


