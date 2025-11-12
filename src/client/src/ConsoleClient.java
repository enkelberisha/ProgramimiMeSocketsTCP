package client.src;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ConsoleClient {
    private final String host;
    private final int port;
    private final String role;
    private final String token;

    public ConsoleClient(String host, int port, String role, String token) {
        this.host = host;
        this.port = port;
        this.role = role;
        this.token = token;
    }

    public void run() throws Exception {
        try (Socket sock = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
             BufferedReader kb = new BufferedReader(new InputStreamReader(System.in))) {

            // Read welcome (or busy message)
            String firstReply = readResponseWithResult(in);

            // If server is busy, exit immediately
            if (firstReply.startsWith("ERR Server busy")) {
                System.out.println("Disconnected: " + firstReply);
                return;
            }

            // Send role
            if ("admin".equalsIgnoreCase(role)) {
                Util.writeln(out, "ROLE admin " + token);
            } else {
                Util.writeln(out, "ROLE user");
            }

            // Read role reply
            firstReply = readResponseWithResult(in);

            // If server refused again, exit
            if (firstReply.startsWith("ERR")) {
                System.out.println("Disconnected: " + firstReply);
                return;
            }

            // ✅ Only show commands if connection is good
            System.out.println("Enter commands (/list, /read f, /upload f, /download f, /delete f, /search kw, /info f, /quit)");

            String line;
            while ((line = kb.readLine()) != null) {
                if (line.startsWith("/upload ")) {
                    String localPath = line.substring(8).trim();
                    Path localFile = Paths.get(localPath);
                    if (!Files.exists(localFile)) {
                        System.out.println("Local file not found: " + localFile);
                        continue;
                    }

                    // Just send the file name (not full path) to server
                    String fileNameOnly = localFile.getFileName().toString();

                    Util.writeln(out, "/upload " + fileNameOnly);
                    readResponse(in); // expect OK READY

                    byte[] data = Files.readAllBytes(localFile);
                    String b64 = Base64.getEncoder().encodeToString(data);
                    Util.writeln(out, b64);
                    readResponse(in);
                } else if (line.startsWith("/download ")) {
                    Util.writeln(out, line);
                    String header = in.readLine();
                    if (header == null || !header.startsWith("OK SIZE ")) {
                        drain(in, header);
                        continue;
                    }
                    int size = Integer.parseInt(header.substring(8).trim());
                    String b64 = in.readLine();
                    byte[] data = Base64.getDecoder().decode(b64);
                    readResponse(in); // read "."
                    String fname = line.substring(10).trim();
                    Files.write(Paths.get(fname), data);
                    System.out.println("Downloaded " + fname + " (" + size + " bytes)");
                } else {
                    Util.writeln(out, line);
                    readResponse(in);
                    if ("/quit".equalsIgnoreCase(line)) break;
                }
            }
        }
    }

    // Normal response reader
    private static void readResponse(BufferedReader in) throws IOException {
        String s;
        while ((s = in.readLine()) != null) {
            if (s.equals(".")) break;
            System.out.println(s);
        }
    }

    // Same as above, but also returns the first line
    private static String readResponseWithResult(BufferedReader in) throws IOException {
        String firstLine = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (firstLine == null) firstLine = line;
            if (line.equals(".")) break;
            System.out.println(line);
        }
        return firstLine == null ? "" : firstLine;
    }

    private static void drain(BufferedReader in, String first) throws IOException {
        if (first != null) System.out.println(first);
        String s;
        while ((s = in.readLine()) != null) {
            System.out.println(s);
            if (s.equals(".")) break;
        }
    }
}
