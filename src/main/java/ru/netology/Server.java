package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.netology.Constants.*;
import static ru.netology.utils.Logger.log;

public class Server {

    private ServerSocket serverSocket;

    public final List<String> validPaths = List.of(SOURCE_1, SOURCE_2, SOURCE_3, SOURCE_4, SOURCE_5, SOURCE_6, SOURCE_7,
            SOURCE_8, SOURCE_9, SOURCE_10, SOURCE_11);

    public Server(int port) {
        try {
            this.serverSocket = new ServerSocket(port, NUMBER_OF_POOLS);
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

    private boolean checkSpecialCases(String mimeType, Path filePath, String path, BufferedOutputStream out,
                                      List<NameValuePair> paramList)
            throws IOException {
        if (path.equals(SOURCE_9)) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write(getOkMessage(mimeType, content.length).getBytes());
            out.write(content);
            out.flush();
            return true;
        } else if (path.equals(SOURCE_8)) {
            log("Значение параметра login = " + getQueryParam(paramList, "login"));
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

        for (int i = 0; i < NUMBER_OF_POOLS; i++) {
            customerPool.submit(this::runOneSocket);
        }
    }

    public List<NameValuePair> getQueryParams(String[] queryString) {
        if (queryString.length == 2) {
            return URLEncodedUtils.parse(queryString[1], null);
        }
        return null;
    }

    public String getQueryParam(List<NameValuePair> arr, String paramName) {
        if (arr != null) {
            List<NameValuePair> resArr = arr.stream().filter(x -> x.getName().equals(paramName)).collect(Collectors.toList());
            if (!resArr.isEmpty()) return resArr.get(0).getValue();
        }
        return null;
    }

    public void runOneSocket() {

        while (true) {
            try (
                    final var socket = serverSocket.accept();
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
                    continue;
                }

                var path = parts[1];
                log("path = " + path);

                var queryStringArr = path.split("\\?");
                Arrays.stream(queryStringArr).forEach(x -> log("queryString = " + x));

                if (queryStringArr.length > 2) {
                    continue;
                }
                path = queryStringArr[0];
                log("Change path to " + path);

                var paramList = getQueryParams(queryStringArr);
                if (paramList != null)
                    paramList.forEach(x -> log("param = " + x.getName() + " ~ " + x.getValue()));

                if (!checkValidPath(path, out)) continue;

                final var filePath = Path.of(".", SOURCE_PATH, path);
                final var mimeType = Files.probeContentType(filePath);
                log("mimeType = " + mimeType);

                if (checkSpecialCases(mimeType, filePath, path, out, paramList)) continue;

                sendMessage(mimeType, filePath, out);

            } catch (IOException e) {
                log(e.getMessage());
            }

        }
    }
}
