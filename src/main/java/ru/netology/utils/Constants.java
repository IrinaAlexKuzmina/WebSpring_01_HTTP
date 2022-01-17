package ru.netology.utils;

public final class Constants {

    public static int NUMBER_OF_POOLS = 64;

    public static final String SOURCE_PATH = "public";

    public static final int PORT = 9999;

    public static final String ERROR_404_MESSAGE = "HTTP/1.1 404 Not Found\r\n" +
            "Content-Length: 0\r\n" +
            "Connection: close\r\n" +
            "\r\n";

}
