package server.src;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Stats {
    private static final ConcurrentMap<String, AtomicLong> perClientMsgs = new ConcurrentHashMap<>();
    private static final Set<String> activeIPs = ConcurrentHashMap.newKeySet();
    private static final AtomicLong totalBytesIn = new AtomicLong();
    private static final AtomicLong totalBytesOut = new AtomicLong();
    private static final AtomicInteger activeConns = new AtomicInteger();
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    public static void onConnect(String ip){ activeConns.incrementAndGet(); activeIPs.add(ip); }
    public static void onDisconnect(String ip, long msgs){ activeConns.decrementAndGet(); /* remain in activeIPs as "seen" */ }
    public static void onMessage(String ip){ perClientMsgs.computeIfAbsent(ip, k-> new AtomicLong()).incrementAndGet(); }
    public static void addBytesIn(long n){ totalBytesIn.addAndGet(n); }
    public static void addBytesOut(long n){ totalBytesOut.addAndGet(n); }

    public static void startPeriodicLogging() {
        timer.scheduleAtFixedRate(() -> {
            try {
                Files.writeString(Paths.get("server_stats.txt"), snapshotPretty());
            } catch (IOException ignored) {}
        }, 10, 10, TimeUnit.SECONDS);
    }

    public static String snapshotPretty() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Statistikat e Serverit @ ").append(Instant.now()).append(" ===\n");
        sb.append("Koneksionet aktive: ").append(activeConns.get()).append("\n");
        sb.append("Klientë aktivë/të parë: ").append(activeIPs.size()).append("\n");
        sb.append("Totali i bajtëve HYRËS: ").append(totalBytesIn.get()).append("\n");
        sb.append("Totali i bajtëve DALËS: ").append(totalBytesOut.get()).append("\n");
        sb.append("Mesazhe për klient:\n");
        perClientMsgs.forEach((k,v)-> sb.append("  ").append(k).append(" -> ").append(v.get()).append("\n"));
        return sb.toString();
    }
}
