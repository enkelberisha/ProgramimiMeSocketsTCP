package server.src;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;


public class ClientHandler implements Runnable {
    //------Configurimet
    private final Socket sock;
    private final Path root;
    private final String adminToken;

    private String role = "user"; // "admin" ose "user"
    private String clientId = "unknown";
    private final AtomicLong messages = new AtomicLong(0);

    public ClientHandler(Socket sock, Path root, String adminToken) {
        this.sock = sock;
        this.root = root;
        this.adminToken = adminToken;
    }

    @Override
    public void run() {
        String ip = sock.getInetAddress().getHostAddress() + ":" + sock.getPort();
        this.clientId = ip;
        Stats.onConnect(ip);
        try (var in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             var out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            Util.writeln(out, "Mireserdhet në FileServer");
            Util.writeln(out, ".");
            String first = in.readLine();
            if (first == null    || !first.startsWith("ROLE")) {
                Util.replyErr(out, "Rreshti i parë duhet të jetë ROLE <admin|user> [token]");
                close();
                return;
            }
            String[] parts = first.split("\\s+");
            if (parts.length >= 2 && parts[1].equalsIgnoreCase("admin")) {
                if (parts.length >= 3 && parts[2].equals(adminToken)) role = "admin";
                else {
                    Util.replyErr(out, "Token admini i pavlefshëm; të zhvendosëm në user");
                    role = "user";
                }
            } else {
                role = "user";
            }
            Util.replyOk(out, "Role=" + role);

            String line;
            while ((line = in.readLine()) != null) {
                messages.incrementAndGet();
                Stats.onMessage(clientId);
                line = line.trim();
                if (line.isEmpty()) { Util.replyErr(out, "Komand e thatë"); continue; }
                if ("/quit".equalsIgnoreCase(line)) { Util.replyOk(out, "Tung"); break; }

                try {
                    CommandProcessor.process(line, role, in, out, root);
                } catch (SocketTimeoutException e) {
                    Util.replyErr(out, "Kohëzgjatja e mosveprimit; mbyllje");
                    break;
                } catch (Exception ex) {
                    Util.replyErr(out, "Exception: " + ex.getMessage());
                }
            }
        } catch (IOException ignored) {
        } finally {
            Stats.onDisconnect(clientId, messages.get());
            close();
        }
    }

    private void close() {
        try { sock.close(); } catch (IOException ignored) {}
    }
}
