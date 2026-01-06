package com.android.llanglator;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Downloader {

    private static final String TAG = "Downloader";

    private Downloader() {}

    // Progress callback (0–100)
    public interface ProgressCallback {
        void onProgress(int percent);
    }

    /**
     * Downloads a file if it does not exist or is smaller than minSizeBytes.
     *
     * Storage layout:
     *   files/
     *     ├── llm/
     *     ├── whisper/
     *     └── tts/
     */
    public static File downloadIfNeeded(
            Context context,
            String modelType,          // "llm", "whisper", "tts"
            String url,
            String fileName,
            long minSizeBytes,
            ProgressCallback progress
    ) throws IOException {

        Context appCtx = context.getApplicationContext();

        File baseDir = new File(appCtx.getFilesDir(), modelType);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new IOException("Failed to create dir: " + baseDir);
        }

        File target = new File(baseDir, fileName);

        if (target.exists() && target.length() >= minSizeBytes) {
            Log.d(TAG, "Model already exists: " + target.getAbsolutePath());
            return target;
        }

        Log.d(TAG, "Downloading: " + fileName);
        return downloadFile(url, target, minSizeBytes, progress);
    }

    /**
     * Low-level HTTP download with temp file and progress reporting.
     */
    private static File downloadFile(
            String urlStr,
            File outFile,
            long minSizeBytes,
            ProgressCallback callback
    ) throws IOException {

        File tmpFile = new File(outFile.getAbsolutePath() + ".tmp");

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(60_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Android-Downloader");

        conn.connect();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + conn.getResponseCode()
                    + " " + conn.getResponseMessage());
        }

        long contentLength = conn.getContentLengthLong();
        long totalRead = 0;
        int lastProgress = -1;

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(tmpFile)) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalRead += read;

                if (contentLength > 0 && callback != null) {
                    int progress = (int) ((totalRead * 100) / contentLength);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        callback.onProgress(progress);
                    }
                }
            }
        }

        // Validate size
        if (tmpFile.length() < minSizeBytes) {
            tmpFile.delete();
            throw new IOException("Downloaded file too small: " + tmpFile.length());
        }

        // Atomic replace
        if (outFile.exists() && !outFile.delete()) {
            tmpFile.delete();
            throw new IOException("Failed to delete old file");
        }

        if (!tmpFile.renameTo(outFile)) {
            tmpFile.delete();
            throw new IOException("Failed to finalize download");
        }

        Log.d(TAG, "Download complete: " + outFile.getAbsolutePath());
        return outFile;
    }
}
