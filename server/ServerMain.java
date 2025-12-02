package server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) {
        int port = 5000;
        QueueManager manager = new QueueManager();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("==========================================");
            System.out.println("  SERVER SMARTQUEUE BERJALAN DI PORT " + port);
            System.out.println("  Menunggu koneksi klien...");
            System.out.println("==========================================");

            while (true) {
                // ServerMaint menerima koneksi
                Socket clientSocket = serverSocket.accept();
                // Membuat thread baru (ClientHandler) untuk setiap klien
                new ClientHandler(clientSocket, manager).start();
            }

        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}