package org.example.model;

import java.time.Duration;

public record DownloadResult(
        String url,
        String fileName,
        long size,
        Duration duration,
        Exception error
) {
    public boolean isSuccess() {
        return error == null;
    }
}
