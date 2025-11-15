package client.src;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        String role = args.length > 2 ? args[2] : "user"; // "admin" or "user"
        String token = args.length > 3 ? args[3] : "";    // admin token if role=admin

        new ConsoleClient(host, port, role, token).run();
    }
}