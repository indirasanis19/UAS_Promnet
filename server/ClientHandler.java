package server;

import java.io.*;
import java.net.Socket;
import java.util.Queue;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final QueueManager queueManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, QueueManager manager) {
        this.socket = socket;
        this.queueManager = manager;
    }

    @Override
    public void run() {
        try {
            // Urutan inisialisasi ObjectOutputStream sebelum ObjectInputStream
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Kirim pesan selamat datang
            out.writeObject("Selamat datang di SmartQueue Server!");
            out.flush();

            String clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            System.out.println("Client terhubung: " + clientInfo);

            while (true) {
                // Server menunggu objek (perintah String) dari klien
                Object obj = in.readObject();

                if (!(obj instanceof String)) {
                    continue;
                }

                String command = (String) obj;
                System.out.println("Menerima perintah dari " + clientInfo + ": " + command);

                if (command.startsWith("ORDER")) {
                    // Format: ORDER|Nama|Menu
                    String[] parts = command.split("\\|");
                    if (parts.length < 3)
                        continue;

                    String name = parts[1]; // Nama Pelanggan dari field 'Pelanggan A'
                    String menu = parts[2];

                    Order order = queueManager.addOrder(name, menu);
                    String response = "Pesanan diterima. Nomor antrian kamu: " + order.getOrderNumber()
                            + ". Antrian total: " + queueManager.getQueueSize();
                    out.writeObject(response);
                    out.flush();

                } else if (command.equals("LIST")) {
                    // Kirim objek Queue<Order> ke klien
                    Queue<Order> orders = queueManager.getOrders();
                    out.writeObject(orders);
                    out.flush();

                } else if (command.equals("EXIT")) {
                    break;
                }
            }

            System.out.println("Client terputus: " + clientInfo);

        } catch (EOFException e) {
            System.out.println("Client terputus secara normal.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error pada ClientHandler: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}