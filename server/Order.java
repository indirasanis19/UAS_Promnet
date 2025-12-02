package server;

import java.io.Serializable;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private int orderNumber;
    private String customerName;
    private String menuItem;
    private long timestamp;

    public Order(int orderNumber, String customerName, String menuItem) {
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.menuItem = menuItem;
        this.timestamp = System.currentTimeMillis();
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMenuItem() {
        return menuItem;
    }

    @Override
    public String toString() {
        return "[" + orderNumber + "] " + customerName + " memesan " + menuItem;
    }
}