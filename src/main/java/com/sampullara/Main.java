package com.sampullara;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static final int PORT = 8080;
    private static final int CONCURRENT_REQUESTS = 1000;
    private static final int TOTAL_REQUESTS = 10000;

    public static void main(String[] args) throws Exception {
        runBenchmark(false);
        runBenchmark(true);
    }

    private static void runBenchmark(boolean useVirtualThreads) throws Exception {
        Server server = createServer(useVirtualThreads);
        server.start();

        // Wait for the server to fully start
        while (!server.isStarted()) {
            Thread.sleep(100);
        }

        System.out.println("Server started. Waiting 2 seconds before sending requests...");
        Thread.sleep(2000);
        try {
            List<Long> responseTimes = sendRequests(useVirtualThreads);
            printStats(responseTimes, useVirtualThreads);
        } finally {
            server.stop();
        }
    }

    private static Server createServer(boolean useVirtualThreads) {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(useVirtualThreads ? Executors.newVirtualThreadPerTaskExecutor() : null);

        Server server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(PORT);
        server.addConnector(connector);

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(CPUIntensiveServlet.class, "/*");
        server.setHandler(servletHandler);

        return server;
    }

    private static List<Long> sendRequests(boolean useVirtualThreads) throws Exception {
        List<Long> responseTimes = new ArrayList<>();
        ExecutorService executorService = useVirtualThreads ?
                Executors.newVirtualThreadPerTaskExecutor() :
                Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        HttpClient client = HttpClient.newBuilder()
                .executor(executorService)
                .build();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + PORT))
                    .GET()
                    .build();

            long startTime = System.nanoTime();
            CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        long endTime = System.nanoTime();
                        long responseTime = Duration.ofNanos(endTime - startTime).toMillis();
                        synchronized (responseTimes) {
                            responseTimes.add(responseTime);
                        }
                    }).exceptionally(e -> {
                        if (e.getCause() instanceof ConnectException) {
                            System.err.println("Connection failed. Server might be overloaded.");
                        } else {
                            e.printStackTrace();
                        }
                        return null;
                    });;

            futures.add(future);

            if (futures.size() >= CONCURRENT_REQUESTS) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                futures.clear();
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        return responseTimes;
    }

    private static void printStats(List<Long> responseTimes, boolean useVirtualThreads) {
        double mean = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = responseTimes.stream().mapToDouble(t -> Math.pow(t - mean, 2)).sum() / responseTimes.size();
        double stdDev = Math.sqrt(variance);

        // Find the max
        long max = responseTimes.stream().max(Long::compareTo).get();

        System.out.println("Results for " + (useVirtualThreads ? "Virtual Threads" : "Platform Threads"));
        System.out.println("Total requests: " + TOTAL_REQUESTS);
        System.out.println("Concurrent requests: " + CONCURRENT_REQUESTS);
        System.out.println("Mean response time: " + mean + " ms");
        System.out.println("Standard deviation: " + stdDev + " ms");
        System.out.println("Max response time: " + max + " ms");
        System.out.println();
    }

    public static class CPUIntensiveServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 1000) {
                // Simulate CPU-intensive task
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Task completed");
        }
    }
}