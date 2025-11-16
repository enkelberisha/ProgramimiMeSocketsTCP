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

            String firstReply = readResponseWithResult(in);

<<<<<<< Updated upstream
            // If server is busy, exit immediately
            if (firstReply.startsWith("ERR Serveri është i zënë")) {
                System.out.println("Lidhja u mbyll: " + firstReply);
=======
            if (firstReply.startsWith("ERR Server busy")) {
                System.out.println("Lidhja u ndërpre: Serveri është i zënë.");
>>>>>>> Stashed changes
                return;
            }

            if ("admin".equalsIgnoreCase(role)) {
                Util.writeln(out, "ROLE admin " + token);
            } else {
                Util.writeln(out, "ROLE user");
            }

            firstReply = readResponseWithResult(in);

            if (firstReply.startsWith("ERR")) {
<<<<<<< Updated upstream
                System.out.println("Lidhja u mbyll: " + firstReply);
                return;
            }

            // ✅ Only show commands if connection is good
            System.out.println(
                    "Shkruani komandat (/list, /read f, /upload f, /download f, /delete f, /search kw, /info f, /quit)");
=======
                System.out.println("Lidhja u ndërpre: " + firstReply);
                return;
            }

            System.out.println("Shkruaj komandat (/list, /read f, /upload f, /download f, /delete f, /search kw, /info f, /quit)");
>>>>>>> Stashed changes

            String line;
            while ((line = kb.readLine()) != null) {


                if (line.startsWith("/upload ")) {
                    String localPath = line.substring(8).trim();
                    Path localFile = Paths.get(localPath);
                    if (!Files.exists(localFile)) {
<<<<<<< Updated upstream
                        System.out.println("Fajlli lokal nuk u gjet: " + localFile);
=======
                        System.out.println("Gabim: Skedari lokal nuk u gjet → " + localFile);
>>>>>>> Stashed changes
                        continue;
                    }

                    String fileNameOnly = localFile.getFileName().toString();

                    Util.writeln(out, "/upload " + fileNameOnly);
                    readResponse(in);

                    byte[] data = Files.readAllBytes(localFile);
                    String b64 = Base64.getEncoder().encodeToString(data);
                    Util.writeln(out, b64);
                    readResponse(in);

                }


                else if (line.startsWith("/download ")) {
                    Util.writeln(out, line);

                    String header = in.readLine();
                    if (header == null || !header.startsWith("OK SIZE ")) {
                        drain(in, header);
                        continue;
                    }

                    int size = Integer.parseInt(header.substring(8).trim());
                    String b64 = in.readLine();
                    byte[] data = Base64.getDecoder().decode(b64);

                    readResponse(in);

                    String fname = line.substring(10).trim();
                    Files.write(Paths.get(fname), data);
<<<<<<< Updated upstream
                    System.out.println("Shkarkuar " + fname + " (" + size + " bytes)");
                } else {
                    Util.writeln(out, line);
                    readResponse(in);
                    if ("/quit".equalsIgnoreCase(line))
                        break;
=======

                    System.out.println("U shkarkua skedari " + fname + " (" + size + " bajta)");

                }


                else {
                    Util.writeln(out, line);
                    readResponse(in);

                    if ("/quit".equalsIgnoreCase(line)) {
                        System.out.println("U shkëpute nga serveri.");
                        break;
                    }
>>>>>>> Stashed changes
                }
            }
        }
    }



    private static void readResponse(BufferedReader in) throws IOException {
        String s;
        while ((s = in.readLine()) != null) {
            if (s.equals("."))
                break;
            System.out.println(s);
        }
    }


    private static String readResponseWithResult(BufferedReader in) throws IOException {
        String firstLine = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (firstLine == null)
                firstLine = line;
            if (line.equals("."))
                break;
            System.out.println(line);
        }
        return firstLine == null ? "" : firstLine;
    }


    private static void drain(BufferedReader in, String first) throws IOException {
        if (first != null)
            System.out.println(first);
        String s;
        while ((s = in.readLine()) != null) {
            System.out.println(s);
            if (s.equals("."))
                break;
        }
    }
}
