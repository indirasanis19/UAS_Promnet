package client;

import java.io.*;
import java.net.Socket;
import java.util.Queue;
import java.util.Scanner;
import server.Order;

public class ClientMain {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5000);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // penting
            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());

            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== SMARTQUEUE CLIENT ===");
                System.out.println("1. Tambah Pesanan");
                System.out.println("2. Lihat Daftar Antrian");
                System.out.println("0. Keluar");
                System.out.print("Pilih: ");

                int pilih = sc.nextInt();
                sc.nextLine();

                if (pilih == 1) {
                    System.out.print("Nama Pemesan: ");
                    String name = sc.nextLine();

                    System.out.print("Menu: ");
                    String menu = sc.nextLine();

                    out.writeObject("ORDER|" + name + "|" + menu);
                    out.flush();
                    System.out.println(">> " + objectIn.readObject());

                } else if (pilih == 2) {
                    out.writeObject("LIST");
                    out.flush();
                    Object obj = objectIn.readObject();
                    Queue<Order> orders = (Queue<Order>) obj;

                    System.out.println("\n===== DAFTAR ANTRIAN =====");
                    if (orders.isEmpty())
                        System.out.println("(Kosong)");
                    else
                        orders.forEach(System.out::println);

                } else if (pilih == 0) {
                    out.writeObject("EXIT");
                    out.flush();
                    break;
                }
            }
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
