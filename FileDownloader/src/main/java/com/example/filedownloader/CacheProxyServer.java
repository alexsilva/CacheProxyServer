package com.example.filedownloader;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

/**
 * Created by alex on 10/09/13.
 */
public class CacheProxyServer implements HttpServerRequestCallback {
    public static String TAG = CacheProxyServer.class.getSimpleName();

    public static int PORT = 5000;
    public static String ADDRESS = String.format("http://localhost:%s", PORT);

    public static final String GET_REGEX = "/file/(.*)/url/(.*).*?";
    public static final String GET_FORMAT = "/file/%s/url/%s";
    public static final String CHARSET = "UTF-8";

    public static final int FILE_GROUP_INDEX = 1;
    public static final int URL_GROUP_INDEX = 2;

    public static final String CACHE_DIR = "media";
    public static final int BYTES_BUFFER = 2048;

    private int cacheSize = 1024 *1024 *25;

    private Context context;
    private AsyncHttpServer server;
    private SimpleDiskCache cache;

    public CacheProxyServer(Context context) {
        this.context = context;
        setupCache();

        server = new AsyncHttpServer();
        server.get(GET_REGEX, this);
    }
    public void listen() {
        server.listen(PORT); // listen on port
    }
    public static String encode(String data) {
        if (Build.VERSION.SDK_INT > 7){ // min sdk 8+
            data = new String(Base64.encode(data.getBytes(), Base64.URL_SAFE));
        } else {
            try { data = URLEncoder.encode(data, CHARSET);
            } catch (UnsupportedEncodingException e){}
        }
        return data;
    }
    public static String decode(String data) {
        if (Build.VERSION.SDK_INT > 7){ // min sdk 8+
            data = new String(Base64.decode(data.getBytes(), Base64.URL_SAFE));
        } else {
            try { data = URLDecoder.decode(data, CHARSET);
            } catch (UnsupportedEncodingException e){}
        }
        return data;
    }
    public String getAbsoluteUrl(String file, String targetUrl) {
        return ADDRESS + String.format(GET_FORMAT, encode(file), encode(targetUrl));
    }
    public AsyncHttpServer getServer(){
        return server;
    }
    // ---------------------------------------------------------------------------------------------
    private void setupCache() {
        if (cache == null){
            File cacheFile = new File(context.getCacheDir().getAbsolutePath() +
                        File.separator + CACHE_DIR);
            try {
                cache = new SimpleDiskCache(cacheFile, 1, cacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }
    // ---------------------------------------------------------------------------------------------
    private void closeInputStream(InputStream stream){
        if (stream != null){
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void closeOutputStream(OutputStream stream){
        if (stream != null){
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // ---------------------------------------------------------------------------------------------
    private void writeBytesBuffer(AsyncHttpServerResponse response,
                                  InputStream inputStream, OutputStream outputStream) {
        byte[] buff = new byte[BYTES_BUFFER];
        int counter = 0;
        try {
            while ((counter = inputStream.read(buff)) != -1){
                response.write(ByteBuffer.wrap(buff, 0, counter));
                outputStream.write(buff, 0, counter);
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            closeOutputStream(outputStream);
        }
    }
    private void writeBytesBuffer(AsyncHttpServerResponse response, InputStream inputStream) {
        byte[] buff = new byte[BYTES_BUFFER];
        int counter = 0;
        try {
            while ((counter = inputStream.read(buff)) != -1){
                response.write(ByteBuffer.wrap(buff, 0, counter));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ---------------------------------------------------------------------------------------------
    @Override
    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
        String key = decode(request.getMatcher().group(FILE_GROUP_INDEX));
        Log.d(TAG, String.format("File name [%s]", key));

        InputStream inputStream = getCacheInputStream(key);

        try {
            response.responseCode(HttpURLConnection.HTTP_OK);

            if (inputStream == null){ // internet file load
                inputStream = getConnInputStream(request, response);
                writeBytesBuffer(response,  new BufferedInputStream(inputStream), cache.put(key));

            } else { // cache file load
                writeBytesBuffer(response,  new BufferedInputStream(inputStream));
            }
        } catch (IOException e){
            e.printStackTrace();

        } finally {
            closeInputStream(inputStream);
            response.close();
        }
    }
    // ---------------------------------------------------------------------------------------------
    public InputStream getConnInputStream(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
        String targetUrl = decode(request.getMatcher().group(URL_GROUP_INDEX));
        Log.d(TAG, String.format("Loading url [%s]", targetUrl));

        InputStream inputStream = null;

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                Log.d(TAG, "Invalid response: " + connection.getResponseCode());
                response.close();
                return null;
            }
            response.setContentType(connection.getHeaderField("Content-Type"));
            // this will be useful to display download percentage. might be -1: server did not report the length
            // int fileLength = connection.getContentLength();
            inputStream = connection.getInputStream();
        } catch (IOException e){
            e.printStackTrace();
        }
        return inputStream;
    }
    // ---------------------------------------------------------------------------------------------
    public InputStream getCacheInputStream(String key) {
        InputStream inputStream = null;
        try {
            SimpleDiskCache.InputStreamEntry streamEntry = cache.getInputStream(key);
            if (streamEntry != null)
                inputStream = streamEntry.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }
    // ---------------------------------------------------------------------------------------------
}
