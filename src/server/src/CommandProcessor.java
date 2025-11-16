package server.src;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.Base64;

public class CommandProcessor {
    private static final Set<String> READ_ONLY = Set.of("/list", "/read", "/download", "/search", "/info");

    public static void process(String cmdLine, String role, BufferedReader in, BufferedWriter out, Path root)
            throws Exception {
        String[] parts = cmdLine.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1] : "";

        boolean isReadOnly = READ_ONLY.contains(cmd);
        if (!isReadOnly && !"admin".equals(role)) {
            Util.replyErr(out, "Hyrja u refuzua (vetëm për admin)");
            return;
        }

        switch (cmd) {
            case "/list" -> handleList(out, root, arg);
            case "/read" -> handleRead(out, root, arg);
            case "/download" -> handleDownload(out, root, arg);
            case "/upload" -> handleUpload(in, out, root, arg);
            case "/delete" -> handleDelete(out, root, arg);
            case "/search" -> handleSearch(out, root, arg);
            case "/info" -> handleInfo(out, root, arg);
            default -> Util.replyErr(out, "Komandë e panjohur");
        }
    }

    private static void handleList(BufferedWriter out, Path root, String sub) throws Exception {
        Path dir = safeResolve(root, sub.isBlank() ? "." : sub);
        if (!Files.isDirectory(dir)) {
            Util.replyErr(out, "Nuk ekziston");
            return;
        }
        try (Stream<Path> s = Files.list(dir)) {
            List<String> items = s
                    .sorted()
                    .map(p -> p.getFileName() + (Files.isDirectory(p) ? "/" : ""))
                    .toList();
            Util.replyOk(out, String.join("\n", items));
        }
    }

    private static void handleRead(BufferedWriter out, Path root, String name) throws Exception {
        Path f = safeResolve(root, name);
        if (!Files.exists(f) || Files.isDirectory(f)) {
            Util.replyErr(out, "Fajlli nuk u gjet");
            return;
        }
        String text = Files.readString(f);
        Stats.addBytesOut(text.getBytes(StandardCharsets.UTF_8).length);
        Util.replyOk(out, text);
    }

    private static void handleDownload(BufferedWriter out, Path root, String name) throws Exception {
        Path f = safeResolve(root, name);
        if (!Files.exists(f) || Files.isDirectory(f)) {
            Util.replyErr(out, "Fajlli nuk u gjet");
            return;
        }
        byte[] bytes = Files.readAllBytes(f);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        Stats.addBytesOut(bytes.length);
        Util.writeln(out, "OK SIZE " + bytes.length);
        Util.writeln(out, b64);
        Util.writeln(out, ".");
    }

    private static void handleUpload(BufferedReader in, BufferedWriter out, Path root, String name) throws Exception {
        if (name.isBlank()) {
            Util.replyErr(out, "Perdorimi: /upload <filename>");
            return;
        }


        Path f = root.resolve(Paths.get(name).getFileName()).normalize();

        Util.writeln(out, "OK READY");
        Util.writeln(out, ".");

        String b64 = in.readLine();
        if (b64 == null)
            throw new EOFException("Duke pritur base64 line");
        byte[] data = Base64.getDecoder().decode(b64);

        Files.createDirectories(f.getParent());
        Files.write(f, data);

        Stats.addBytesIn(data.length);
        Util.replyOk(out, "U ngarkuan " + f.getFileName() + " (" + data.length + " bytes)");
    }

    private static void handleDelete(BufferedWriter out, Path root, String name) throws Exception {
        Path f = safeResolve(root, name);
        if (!Files.exists(f)) {
            Util.replyErr(out, "Fajlli nuk u gjet");
            return;
        }
        if (Files.isDirectory(f)) {
            Util.replyErr(out, "Refuzimi për të fshirë direktoriumet");
            return;
        }
        Files.delete(f);
        Util.replyOk(out, "U fshi " + name);
    }

    private static void handleSearch(BufferedWriter out, Path root, String kw) throws Exception {
        if (kw.isBlank()) {
            Util.replyErr(out, "Perdorimi: /search <keyword>");
            return;
        }
        List<String> hits = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            s.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String n = p.getFileName().toString();
                        if (n.toLowerCase().contains(kw.toLowerCase()))
                            hits.add(root.relativize(p).toString());
                    });
        }
        Util.replyOk(out, String.join("\n", hits));
    }

    private static void handleInfo(BufferedWriter out, Path root, String name) throws Exception {
        Path f = safeResolve(root, name);
        if (!Files.exists(f) || Files.isDirectory(f)) {
            Util.replyErr(out, "Fajlli nuk u gjet");
            return;
        }
        var attr = Files.readAttributes(f, "basic:*");
        String res = "name=" + f.getFileName() +
                "\nsize=" + Files.size(f) +
                "\ncreated=" + attr.get("creationTime") +
                "\nmodified=" + attr.get("lastModifiedTime");
        Util.replyOk(out, res);
    }

    private static Path safeResolve(Path root, String userPath) throws IOException {
        Path p = root.resolve(userPath).normalize();
        if (!p.startsWith(root))
            throw new IOException("Udha traversale eshte e bllokuar");
        return p;
    }
}
