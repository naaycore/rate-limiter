package com.prodigious.httpProxy;

import com.prodigious.Configuration.domain.RateLimiterConfiguration;
import com.prodigious.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class RequestHandler {
    private final ExecutorService threadPool;
    private volatile boolean running = true;
    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<String, RateLimiter> endpointConfigMap;

    public RequestHandler(int port, int maxThreads) throws IOException {
        endpointConfigMap =
                RateLimiterConfiguration.getInstance()
                                        .getEndpointRateLimiterMap();

        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
    }

    public void startHandler() {
        while (running) {
            submitRequest();
        }
    }

    public void stopHandler(){
        log.info("Server terminating...");
        running = false;
        try{
            if(serverSocket != null){
                serverSocket.close();
                log.info("Server terminated");
            }
        }catch (IOException e){
            log.error("Error while attempting to shutdown server", e);
        }
    }

    private void submitRequest() {
        try {
            Socket client = serverSocket.accept();
            threadPool.submit(() -> handleClient(client));
        } catch (SocketException e) {
            if (!running) {
                log.info("Server socket closed, stopping accept loop");
            } else {
                log.error("Unknown error", e);
            }
        } catch (IOException e) {
            log.error("Error streaming data from client");
        }
    }

    private void handleClient(Socket client) {
        try (client; Socket upServer = new Socket()) {
            upServer.connect(
                    new InetSocketAddress("127.0.0.1", 8081));

            BufferedReader clientIn =
                    new BufferedReader(
                            new InputStreamReader(client.getInputStream()));

            OutputStream clientOut = client.getOutputStream();
            InputStream upstreamIn = upServer.getInputStream();
            OutputStream upstreamOut = upServer.getOutputStream();

            Thread clientToUpstreamThread = createPipeThread(
                    clientIn,
                    upstreamOut,
                    clientOut,
                    upServer,
                    "client-to-upstream"
            );

            Thread upstreamToClientThread = createPipeThread(
                    upstreamIn,
                    clientOut,
                    client,
                    "upstream-to-client"
            );

            clientToUpstreamThread.start();
            upstreamToClientThread.start();

            clientToUpstreamThread.join();
            upstreamToClientThread.join();
        } catch (IOException e) {
            log.error("Error streaming from client");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Thread createPipeThread(
            InputStream in,
            OutputStream out,
            Socket socket,
            String name
    ) {
        return new Thread(() -> {
            try {
                pipe(in, out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.shutdownOutput();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, name);
    }

    private Thread createPipeThread(
            BufferedReader reader,
            OutputStream outputStream,
            OutputStream clientOutputStream,
            Socket socket,
            String name
    ) {
        return new Thread(() -> {
            try{
                rateLimitPipe(reader, outputStream, clientOutputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.shutdownOutput();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, name);
    }

    private void rateLimitPipe(
            BufferedReader reader,
            OutputStream out,
            OutputStream clientOut
    ) {
        try {
            String line;
            String sessionId;
            String path = "";
            int i = 0;
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if(i == 0){
                    String[] parts = line.split(" ");
                    if(parts.length != 3){
                        throw new HttpProxyException("Malformed request line");
                    }
                    int qIndex = parts[1].indexOf("?");
                    if(qIndex == -1){
                        path = parts[1].substring(1);
                    }else{
                        path = parts[1].substring(1, qIndex);
                    }

                }

                i = -1;

                if (line.contains("Cookie")) {
                    sessionId = HttpProxyUtil.extractSessionId(line);

                    rateLimit(sessionId, path);
                    log.debug("Request permitted");
                }

                String toWrite = line + "\r\n";

                out.write(toWrite.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (HttpProxyException e) {
            if ("Too many requests".equals(e.getMessage())) {
                if (clientOut != null) {
                    try {
                        writeRateLimitResponse(clientOut);
                    } catch (IOException ioException) {
                        log.error("Failed to send 429 response", ioException);
                    }
                }
            }
            log.info(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void rateLimitPipe(BufferedReader reader, OutputStream out) {
        rateLimitPipe(reader, out, null);
    }

    private void writeRateLimitResponse(OutputStream clientOut) throws IOException {
        byte[] bodyBytes = "Too many requests".getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 429 Too Many Requests\r\n"
                + "Content-Type: text/plain; charset=utf-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        clientOut.write(response.getBytes(StandardCharsets.UTF_8));
        clientOut.write(bodyBytes);
        clientOut.flush();
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            out.flush();
        }
    }

    private void rateLimit(String sessionId, String path)
            throws HttpProxyException {
        RateLimiter rateLimiter = endpointConfigMap.get(path);
        if(!rateLimiter.allow(sessionId, path)){
            throw new HttpProxyException("Too many requests");
        }
    }
}
