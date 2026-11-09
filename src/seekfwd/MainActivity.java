package dev.nuri.seekfwd;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        android.os.StrictMode.setThreadPolicy(
            new android.os.StrictMode.ThreadPolicy.Builder().permitAll().build());
        SharedPreferences sp = getSharedPreferences("s", 0);
        long now = System.currentTimeMillis();
        int cnt = (now - sp.getLong("last", 0) < 1000) ? sp.getInt("cnt", 0) + 1 : 1;
        sp.edit().putLong("last", now).putInt("cnt", cnt).apply();
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        if (sp.getLong("last", 0) != now) { finish(); return; }
        StringBuilder cmd = new StringBuilder(
            "dumpsys activity activities | grep topResumed | grep -q com.google.android.youtube.tv || exit 0; ");
        for (int i = 0; i < cnt; i++) cmd.append("input keyevent 22; sleep 0.2; ");
        cmd.append("input keyevent 126");
        try {
            for (int i = 0; i < 60; i++) {
                if (!Shizuku.pingBinder()) break;
                int p = Shizuku.checkSelfPermission();
                if (p == PackageManager.PERMISSION_GRANTED) {
                    Method m = Shizuku.class.getDeclaredMethod("newProcess",
                        String[].class, String[].class, String.class);
                    m.setAccessible(true);
                    m.invoke(null, new String[]{"sh","-c", cmd.toString()}, null, null);
                    break;
                } else if (p == PackageManager.PERMISSION_DENIED) {
                    Shizuku.requestPermission(0);
                    break;
                }
                Thread.sleep(50);
            }
        } catch (Throwable t) { Log.e("sz", "fail", t); }
        finish();
    }
}
