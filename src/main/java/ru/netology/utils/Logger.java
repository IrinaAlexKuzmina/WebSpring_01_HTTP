package ru.netology.utils;

import java.time.format.DateTimeFormatter;

public class Logger {
    public static void log(String text) {
        System.out.println(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss:SSS")) + " (" +
                Thread.currentThread().getName() + ") " + text);
    }
}
