package client.src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class FileIO {

    public static String readText(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static void writeText(Path path, String text) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    public static String readBase64(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static void writeBase64(Path path, String base64) throws IOException {
        byte[] data = Base64.getDecoder().decode(base64);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, data);
    }
}
