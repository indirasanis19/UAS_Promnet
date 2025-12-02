package server;

import java.util.LinkedList;
import java.util.Queue;

public class QueueManager {
    // LinkedList digunakan sebagai implementasi Queue
    private final Queue<Order> orderQueue = new LinkedList<>();
    private int counter = 1;

    // Metode synchronized untuk operasi aman dari multi-threading
    public synchronized Order addOrder(String name, String menu) {
        Order order = new Order(counter++, name, menu);
        orderQueue.add(order);
        System.out.println("Pesanan baru ditambahkan: " + order);
        return order;
    }

    // Mengembalikan salinan antrian saat ini
    public synchronized Queue<Order> getOrders() {
        return new LinkedList<>(orderQueue);
    }

    public synchronized int getQueueSize() {
        return orderQueue.size();
    }
}