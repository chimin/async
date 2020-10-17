package c4compile.async.test;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.awt.Frame.MAXIMIZED_BOTH;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.iterate;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class Application {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.custom()
            .useSystemProperties()
            .setMaxConnPerRoute(Integer.MAX_VALUE)
            .setMaxConnTotal(Integer.MAX_VALUE)
            .build();

    @SneakyThrows
    public static void main(String[] args) {
        Map<String, Integer> targets = new LinkedHashMap<>();
        targets.put("java-spring-boot", 8001);
        targets.put("kotlin-spring-boot-reactive", 8005);
        targets.put("cs-aspnet-core", 8002);
        targets.put("nodejs-restify", 8003);
        targets.put("nodejs-express", 8004);

        Map<String, List<Long>> results = targets.entrySet().stream()
                .collect(toMap(Entry::getKey, Application::performFullTest));

        XYChart chart = new XYChartBuilder()
                .title("Async Test")
                .yAxisTitle("Completed #")
                .xAxisTitle("Elapsed (ms)")
                .build();

        results.forEach((key, value) ->
                chart.addSeries(key,
                        value.stream().mapToDouble(Long::doubleValue).toArray(),
                        iterate(0, i -> i < value.size(), i -> i + 1).mapToDouble(Integer::doubleValue).toArray()));

        JFrame frame = new SwingWrapper<>(chart).displayChart();
        frame.setExtendedState(frame.getExtendedState() | MAXIMIZED_BOTH);
    }

    @SneakyThrows
    private static List<Long> performFullTest(Entry<String, Integer> entry) {
        Process process = startServer(entry.getKey(), entry.getValue());
        try {
            performTest(entry.getValue(), 10);
            return performTest(entry.getValue(), 100);
        } finally {
            if (process != null) {
                process.descendants().forEach(processHandle -> {
                    if (!processHandle.destroy()) {
                        processHandle.destroyForcibly();
                    }
                });

                process.destroy();
                if (!process.waitFor(5, SECONDS)) {
                    process.destroyForcibly();
                }
            }
        }
    }

    @SneakyThrows
    private static List<Long> performTest(int port, int count) {
        String requestBody = OBJECT_MAPPER.writeValueAsString(TokenCreateRequest.builder()
                .userId("somebody")
                .build());

        ExecutorService executor = newCachedThreadPool();
        List<Future<Long>> futures = iterate(0, i -> i < count, i -> i + 1)
                .map(i -> executor.submit(() -> {
                    long startTime = currentTimeMillis();
                    try {
                        System.out.printf("#%d started%n", i + 1);
                        TokenCreateResponse response = performRequest(port, requestBody);
                        long elapsed = currentTimeMillis() - startTime;
                        JWT.decode(response.getToken());
                        System.out.printf("#%d completed in %sms%n", i + 1, elapsed);
                        return elapsed;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }))
                .collect(toList());

        List<Long> elapseds = futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .sorted()
                .collect(toList());

        executor.shutdown();
        executor.awaitTermination(1, MINUTES);

        return elapseds;
    }

    @SneakyThrows
    private static TokenCreateResponse performRequest(int port, String requestBody) {
        HttpPost request = new HttpPost("http://localhost:" + port + "/token");
        request.setEntity(new StringEntity(requestBody, APPLICATION_JSON));

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            boolean success = response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300;
            return success ? OBJECT_MAPPER.readValue(response.getEntity().getContent(), TokenCreateResponse.class) : null;
        }
    }

    @SneakyThrows
    private static boolean ping(int port) {
        try {
            HttpGet request = new HttpGet("http://localhost:" + port);
            HTTP_CLIENT.execute(request).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @SneakyThrows
    private static Process startServer(String name, int port) {
        if (ping(port)) {
            return null;
        }

        File shell = new File(System.getenv("ComSpec"));
        File directory = new File("../token-server/" + name);
        Process process = new ProcessBuilder(shell.toString(), "/c", "start.cmd")
                .directory(directory.getAbsoluteFile())
                .inheritIO()
                .start();

        long startTime = currentTimeMillis();
        while (!ping(port)) {
            if (currentTimeMillis() - startTime > 60_000) {
                process.destroyForcibly();
                throw new IllegalStateException("Failed to start server " + name);
            }
            Thread.sleep(1000);
        }

        return process;
    }

}
