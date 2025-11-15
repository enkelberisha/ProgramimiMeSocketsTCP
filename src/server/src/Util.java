package server.src;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class Util {
    public static void writeln(BufferedWriter out, String s) throws IOException {
        out.write(s);
        out.write("\n");
        out.flush();
    }
    public static void replyOk(BufferedWriter out, String body) throws IOException {
        writeln(out, "OK " + (body == null ? "" : body));
        writeln(out, ".");
    }
    public static void replyErr(BufferedWriter out, String body) throws IOException {
        writeln(out, "ERR " + (body == null ? "" : body));
        writeln(out, ".");
    }
}
