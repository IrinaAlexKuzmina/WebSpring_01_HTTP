package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static ru.netology.utils.Constants.*;
import static ru.netology.utils.Logger.log;

public class Server {

    private BlockingQueue<Socket> sockets;
    private ServerSocket serverSocket;

    public final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public Server(int port) {
        try {
            // ArrayBlockingQueue - так как FIFO
            // и в принципе фиксированный размер нам подходит, так как количество пулов все равно ограничено
            // поэтому предполагается что она не будет расти безразмерно
            // но очередь может возникнуть если запросы будут добавляться быстрее, чем обрабатываться
            sockets = new ArrayBlockingQueue<>(NUMBER_OF_POOLS);
            this.serverSocket = new ServerSocket(port, NUMBER_OF_POOLS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Server() {
        try {
            sockets = new ArrayBlockingQueue<>(NUMBER_OF_POOLS);
            this.serverSocket = new ServerSocket();
            serverSocket.bind(serverSocket.getLocalSocketAddress(), NUMBER_OF_POOLS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getOkMessage(String mimeType, long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    private String getErrorMessage() {
        return ERROR_404_MESSAGE;
    }

    private boolean checkValidPath(String path, BufferedOutputStream out) throws IOException {
        if (!validPaths.contains(path)) {
            out.write(getErrorMessage().getBytes());
            out.flush();
            return false;
        }
        return true;
    }

    private boolean checkSpecialCases(String mimeType, Path filePath, String path, BufferedOutputStream out)
            throws IOException {
        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write(getOkMessage(mimeType, content.length).getBytes());
            out.write(content);
            out.flush();
            return true;
        }
        return false;
    }

    private void sendMessage(String mimeType, Path filePath, BufferedOutputStream out) throws IOException {
        out.write(getOkMessage(mimeType, Files.size(filePath)).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    public void runPoolOfSockets() {

        final ExecutorService customerPool = Executors.newFixedThreadPool(NUMBER_OF_POOLS);

        while (true) {
            try {
                final var socket = serverSocket.accept();
                log("New socket !!!");
                sockets.put(socket);
                customerPool.execute(this::runOneSocket);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void runOneSocket() {
        log("Start runOneSocket... sockets.size = " + sockets.size());
        try (final var socket = sockets.take();
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            log("requestLine = " + requestLine);

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];

            if (!checkValidPath(path, out)) return;

            final var filePath = Path.of(".", SOURCE_PATH, path);
            final var mimeType = Files.probeContentType(filePath);

            if (checkSpecialCases(mimeType, filePath, path, out)) return;

            sendMessage(mimeType, filePath, out);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
