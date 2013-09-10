package com.example.filedownloader;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends Activity {
    public static String TAG = "com.example.filedownloader";
    public static String URL = "http://music18.som13.com/2766f9f75771e1ddbbbc3fcc29f36801";

    MediaPlayer player;
    CacheProxyServer cacheProxyServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new MediaPlayer();

        cacheProxyServer = new CacheProxyServer(this);
        cacheProxyServer.listen();

        setupPlayer();
    }
    public void setupPlayer() {
        try {
            String url = cacheProxyServer.getAbsoluteUrl("music", URL);
            Log.d(TAG, url);

            player.reset();
            player.setDataSource(url);

            Thread th = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        player.prepare();
                        player.start();
                    } catch (IOException e) {
                        Log.d(TAG, " Media prepare: " + e.getMessage());
                    }
                }
            });
            th.start();
        } catch (IOException e) {
            Log.d(TAG, "**** Setup Exception ****");
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy() {
        try {
            player.release();
        } finally {
            super.onDestroy();
        }
    }
}
