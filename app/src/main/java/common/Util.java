package common;

import android.os.AsyncTask;
import android.util.Log;

import com.github.pertempto.valvetimer.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import models.Timer;

public class Util {
    private static final String LOG_TAG = "Util";

    public static void runInBackground(final Runnable task) {
        new BackgroundTask(new BackgroundTask.BackgroundTaskCallback() {
            @Override
            public void call() {
                task.run();
            }
        }).execute();
    }

    /* Get timer from server with given name */
    public static Timer getTimer(String timerName) {
        // code modified from https://stackoverflow.com/a/8655039
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(String.format("%s?q={\"name\":\"%s\"}", BuildConfig.RESTDB_URL, timerName));

            urlConnection = (HttpURLConnection) url
                    .openConnection();

            urlConnection.setRequestProperty("content-type", "application/json");
            urlConnection.setRequestProperty("x-apikey", BuildConfig.RESTDB_KEY);
            urlConnection.setRequestProperty("cache-control", "no-cache");

            InputStreamReader isw = new InputStreamReader(urlConnection.getInputStream());
            int data = isw.read();
            StringBuilder sb = new StringBuilder();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                sb.append(current);
            }
            isw.close();
            String content = sb.toString();
            JSONArray jsonArray = new JSONArray(content);
            if (jsonArray.length() == 0) {
                return null;
            }
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            String id = jsonObject.getString("_id");
            int end = jsonObject.getInt("end");
            int lastSeen = jsonObject.getInt("last_seen");

            return new Timer(id, timerName, end, lastSeen);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    /* Set the timer length to the given length in seconds */
    public static Timer setTimerLength(Timer timer, int seconds) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(String.format("%s/%s", BuildConfig.RESTDB_URL, timer.getId()));

            urlConnection = (HttpURLConnection) url
                    .openConnection();

            urlConnection.setRequestMethod("PATCH");
            urlConnection.setRequestProperty("content-type", "application/json");
            urlConnection.setRequestProperty("x-apikey", BuildConfig.RESTDB_KEY);
            urlConnection.setRequestProperty("cache-control", "no-cache");
            urlConnection.setDoOutput(true);
            OutputStreamWriter osw = new OutputStreamWriter(urlConnection.getOutputStream());
            int now = (int) (System.currentTimeMillis() / 1000);
            int newEnd = now + seconds;
            osw.write(String.format("{\"end\":%d}", newEnd));
            osw.flush();
            urlConnection.connect();

            InputStreamReader isw = new InputStreamReader(urlConnection.getInputStream());
            int data = isw.read();
            StringBuilder sb = new StringBuilder();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                sb.append(current);
            }
            isw.close();
            String content = sb.toString();
            Log.d(LOG_TAG, String.format("response: %s", content));

            JSONObject jsonObject = new JSONObject(content);
            String id = jsonObject.getString("_id");
            int end = jsonObject.getInt("end");
            int lastSeen = jsonObject.getInt("last_seen");
            return new Timer(id, timer.getName(), end, lastSeen);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Log.d(LOG_TAG, String.format("code: %d", urlConnection.getResponseCode()));
                Log.d(LOG_TAG, String.format("message: %s", urlConnection.getResponseMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }
}

class BackgroundTask extends AsyncTask<Void, Void, Void> {
    private BackgroundTaskCallback callback;

    BackgroundTask(BackgroundTaskCallback callback) {
        this.callback = callback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        callback.call();
        return null;
    }

    public interface BackgroundTaskCallback {
        void call();
    }
}