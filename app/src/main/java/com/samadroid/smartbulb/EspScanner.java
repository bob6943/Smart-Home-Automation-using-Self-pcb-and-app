package com.samadroid.smartbulb;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EspScanner — Same WiFi pe ESP32 ka IP dhundhta hai
 * Usage:
 *   EspScanner.scan(context, ip -> {
 *       // ip = "192.168.1.105" ya null agar nahi mila
 *   });
 */
public class EspScanner {

    public interface ScanCallback {
        void onResult(String espIp); // null = nahi mila
    }

    private static final String TAG     = "EspScanner";
    private static final int    TIMEOUT = 800; // ms per IP

    public static void scan(Context context, ScanCallback callback) {

        // Phone ka current IP lo
        WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        int ipInt = wm.getConnectionInfo().getIpAddress();

        if (ipInt == 0) {
            Log.w(TAG, "WiFi connected nahi hai");
            callback.onResult(null);
            return;
        }

        // IP bytes nikalo (little-endian)
        String phoneIp = String.format("%d.%d.%d.%d",
                (ipInt & 0xff),
                (ipInt >> 8  & 0xff),
                (ipInt >> 16 & 0xff),
                (ipInt >> 24 & 0xff));

        // Subnet base nikalo — e.g. "192.168.1."
        String subnet = phoneIp.substring(0, phoneIp.lastIndexOf('.') + 1);
        Log.d(TAG, "Scanning subnet: " + subnet + "* (phone=" + phoneIp + ")");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(20); // 20 parallel scans
        AtomicBoolean found  = new AtomicBoolean(false);

        for (int i = 1; i <= 254; i++) {
            final String ip = subnet + i;
            pool.submit(() -> {
                if (found.get()) return; // Mil gaya, baaki skip karo

                try {
                    String url = "http://" + ip + "/ping";
                    Request request = new Request.Builder().url(url).get().build();
                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        JSONObject json = new JSONObject(body);

                        if ("SmartBulb".equals(json.optString("device"))) {
                            if (found.compareAndSet(false, true)) {
                                Log.d(TAG, "ESP32 mila: " + ip);
                                pool.shutdownNow();
                                callback.onResult(ip);
                            }
                        }
                    }
                    response.close();
                } catch (Exception ignored) {
                    // Yeh IP nahi hai — next
                }
            });
        }

        // Agar 15 sec mein nahi mila toh null return karo
        pool.shutdown();
        new Thread(() -> {
            try {
                if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
                if (!found.get()) {
                    Log.w(TAG, "ESP32 nahi mila subnet mein");
                    callback.onResult(null);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }
}
