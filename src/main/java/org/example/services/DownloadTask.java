package org.example.services;

import org.example.model.DownloadResult;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;

public class DownloadTask {
    private final HttpClient client;
    private final RetryPolicy retryPolicy;

    public DownloadTask(HttpClient client, RetryPolicy retryPolicy) {
        this.client = client;
        this.retryPolicy = retryPolicy;
    }

    DownloadResult download(String url, Path directory) {
        Instant start = Instant.now();
        try {
            return retryPolicy.execute(() -> doDownload(url, directory, start));
        } catch (Exception e) {
            return new DownloadResult(
                    url,
                    safeFileName(url),
                    0,
                    Duration.between(start, Instant.now()),
                    e
            );
        }

    }
    private DownloadResult doDownload(String url, Path directory, Instant start) throws Exception {
        String fileName = safeFileName(url);
        Path target = resolveUnique(directory.resolve(fileName));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }
        long size;
        try(InputStream in = response.body()) {
            size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new DownloadResult(
                url,
                target.getFileName().toString(),
                size,
                Duration.between(start, Instant.now()),
                null
        );
    }
    private String safeFileName(String url){
        String path = URI.create(url).getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "donwload-" + System.nanoTime();
        }
        return Path.of(path).getFileName().toString();
    }
    private Path resolveUnique(Path path) throws Exception {
        if (!Files.exists(path)) return path;
        int counter = 1;
        String name = path.getFileName().toString() ;
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        while (true) {
            Path newPath = path.getParent()
                    .resolve(base + "(" + counter + ")" + ext);

            if (!Files.exists(newPath)) return newPath;
            counter++;
        }
    }
}
