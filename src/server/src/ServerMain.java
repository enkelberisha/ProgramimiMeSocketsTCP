package server.src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    //------Configurimet
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("FILESRV_PORT", "9090"));
    public static final String BIND_ADDR = System.getenv().getOrDefault("FILESRV_BIND", "0.0.0.0");
    public static final String ADMIN_TOKEN = System.getenv().getOrDefault("FILESRV_ADMIN_TOKEN", "rrjetakompjuterike");
    public static final Path ROOT = Paths.get(System.getenv().getOrDefault("FILESRV_ROOT", "server_storage")).toAbsolutePath();
    public static final int CLIENT_SO_TIMEOUT_MS = 30_000;

    //------Numri i koneksionev me ndrru:
    public static final int MAX_CONNECTIONS = 4;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(ROOT);
        System.out.println("[Server] Root: " + ROOT);
        System.out.println("[Server] Addresa: " + BIND_ADDR + " : " + PORT);
        System.out.println("[Server] Numri maximal i lidhjeve: " + MAX_CONNECTIONS);


        var pool = new ThreadPoolExecutor(
                MAX_CONNECTIONS,
                MAX_CONNECTIONS,
                30, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("client-worker-" + t.getId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        Stats.startPeriodicLogging();

        new Thread(() -> {
            try (var br = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    String line = br.readLine();
                    if (line == null) break;
                    if (line.trim().equalsIgnoreCase("stats")) {
                        System.out.println(Stats.snapshotPretty());
                    }
                }
            } catch (IOException ignored) {}
        }, "server-console").start();

        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(BIND_ADDR, PORT));
            while (true) {
                Socket s = server.accept();
                s.setSoTimeout(CLIENT_SO_TIMEOUT_MS);

                if (pool.getActiveCount() >= MAX_CONNECTIONS) {

                    try (var out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
                        out.write("ERR Server busy, try again later\n");
                        out.write(".\n");
                        out.flush();
                    } catch (IOException ignored) {}
                    s.close();
                    continue;
                }


                pool.execute(new ClientHandler(s, ROOT, ADMIN_TOKEN));
            }
        }
    }
}
