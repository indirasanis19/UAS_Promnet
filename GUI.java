import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.geom.Ellipse2D;

public class GUI extends JFrame {

    // --- Variabel Socket ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream objectIn;
    // --- End Socket Variables ---

    // --- SKEMA WARNA ---
    private final Color DARK_BG = new Color(30, 31, 37);
    private final Color MEDIUM_BG = new Color(48, 51, 61);
    private final Color TEXT_COLOR = new Color(255, 255, 255);
    private final Color ACCENT_COLOR = new Color(255, 107, 107);
    private final Color INACTIVE_TAB = new Color(133, 137, 153);
    private final Color SIDEBAR_BG = new Color(20, 20, 24);
    private final Color CART_BG = new Color(48, 51, 61);
    private final Color INACTIVE_COLOR = new Color(133, 137, 153);

    private JTextArea connectionStatusArea;
    private JTextField dummyNameField = new JTextField("Pelanggan A");

    private static class MenuItem {
        String name;
        double price;
        String imagePath;

        public MenuItem(String name, double price, String imagePath) {
            this.name = name;
            this.price = price;
            this.imagePath = imagePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            MenuItem menuItem = (MenuItem) o;
            return name.equals(menuItem.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private Map<MenuItem, Integer> cartItems = new LinkedHashMap<>();
    private JPanel cartItemsPanel;
    private JPanel itemsContainer;
    private JLabel subTotalLabel;

    // Data Menu
    private List<MenuItem> menuItems = new ArrayList<>(Arrays.asList(
            new MenuItem("Spicy seasoned seafood noodles", 4.58,
                    "images/seafood_noodles.jpg"),
            new MenuItem("Salted Pasta with mushroom sauce", 2.69, "images/pasta.jpg"),
            new MenuItem("Beef dumpling in hot and sour soup", 3.48, "images/dumpling.jpg"),
            new MenuItem("Healthy noodle with spinach leaf", 3.29, "images/spinach_noodle.jpg"),
            new MenuItem("Hot spicy fried rice with omelet", 3.59, "images/fried_rice.jpg"),
            new MenuItem("Spicy noodle with  omelette", 3.59, "images/instant_noodle.jpg")));

    public GUI() {
        setTitle("FINE Resto");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout());

        connectionStatusArea = new JTextArea(5, 20);
        connectionStatusArea.setEditable(false);
        connectionStatusArea.setBackground(DARK_BG.darker());
        connectionStatusArea.setForeground(TEXT_COLOR);

        // --- 1. Panel Konten Utama (KIRI: Menu | KANAN: Cart) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(splitPane.getWidth() * 2 / 3); // Awalnya 2/3 untuk menu, 1/3 untuk cart
        splitPane.setDividerSize(5);
        splitPane.setBackground(DARK_BG);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // Panel Kiri: Menu
        splitPane.setLeftComponent(createMenuPanel());

        // Panel Kanan: Keranjang
        splitPane.setRightComponent(createCartPanel());

        // Wrapper untuk split pane dengan padding
        JPanel mainContentWrapper = new JPanel(new BorderLayout());
        mainContentWrapper.setOpaque(false);
        mainContentWrapper.setBorder(new EmptyBorder(10, 20, 10, 20));
        mainContentWrapper.add(splitPane, BorderLayout.CENTER);

        add(mainContentWrapper, BorderLayout.CENTER);

        // --- 2. Status Koneksi di Bawah (Footer) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 10, 20));

        JScrollPane statusScrollPane = new JScrollPane(connectionStatusArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(INACTIVE_TAB),
                "Status Koneksi & Log Server",
                0, 0,
                new Font("SansSerif", Font.PLAIN, 12),
                INACTIVE_COLOR));

        bottomPanel.add(statusScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        // --- End Status Koneksi ---

        setVisible(true);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.65));

        new Thread(() -> setupConnection()).start();
    }

    // =======================================================
    // --- NETWORKING METHODS ---
    // =======================================================

    private void setupConnection() {
        SwingUtilities.invokeLater(() -> connectionStatusArea.append("Mencoba koneksi ke server...\n"));
        try {
            socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            objectIn = new ObjectInputStream(socket.getInputStream());

            Object welcome = objectIn.readObject();
            if (welcome instanceof String) {
                SwingUtilities.invokeLater(() -> connectionStatusArea.append("Status: " + (String) welcome + "\n"));
            }

        } catch (EOFException e) {
            SwingUtilities.invokeLater(() -> connectionStatusArea.append("Koneksi ditutup oleh server.\n"));
        } catch (Exception e) {
            SwingUtilities.invokeLater(
                    () -> connectionStatusArea.append("Gagal koneksi ke server. Pastikan ServerMain berjalan.\n"));
        }
    }

    private void processOrder() {
        if (out == null || socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(this, "Koneksi ke server belum siap atau terputus.");
            return;
        }

        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang pesanan kosong.");
            return;
        }

        String name = dummyNameField.getText().trim();
        StringBuilder menuSummary = new StringBuilder();

        for (Map.Entry<MenuItem, Integer> entry : cartItems.entrySet()) {
            MenuItem item = entry.getKey();
            Integer qty = entry.getValue();
            menuSummary.append(item.name).append(" (x").append(qty).append(") ($")
                    .append(String.format("%.2f", item.price * qty)).append("); ");
        }

        String finalMenu = menuSummary.toString().replaceAll("; $", "");

        new Thread(() -> {
            try {
                out.writeObject("ORDER|" + name + "|" + finalMenu);
                out.flush();

                Object response = objectIn.readObject();
                if (response instanceof String) {
                    SwingUtilities.invokeLater(() -> {
                        connectionStatusArea.append("Client: Pesanan dikirim.\n");
                        connectionStatusArea.append("Server Response: " + (String) response + "\n");
                        cartItems.clear();
                        refreshCartPanelDisplay();
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(
                        () -> connectionStatusArea.append("Error saat mengirim pesanan: " + ex.getMessage() + "\n"));
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
    private void lihatAntrian() {
        if (out == null || socket == null || socket.isClosed()) {
            JOptionPane.showMessageDialog(this, "Koneksi ke server belum siap atau terputus.");
            return;
        }

        new Thread(() -> {
            try {
                out.writeObject("LIST");
                out.flush();

                Object obj = objectIn.readObject();
                Queue<server.Order> orders = (Queue<server.Order>) obj;

                SwingUtilities.invokeLater(() -> {
                    connectionStatusArea.append("===== Daftar Antrian Terbaru (" + orders.size() + ") =====\n");
                    if (orders.isEmpty()) {
                        connectionStatusArea.append("(Kosong)\n");
                    } else {
                        orders.forEach(order -> connectionStatusArea.append(order.toString() + "\n"));
                    }
                    connectionStatusArea.append("==========================================\n");
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(
                        () -> connectionStatusArea.append("Error saat melihat antrian: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void exitApplication() {
        if (out != null) {
            try {
                out.writeObject("EXIT");
                out.flush();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    // =======================================================
    // --- UI COMPONENTS ---
    // =======================================================

    private JPanel createMenuPanel() {
        JPanel menuContainer = new JPanel(new BorderLayout());
        menuContainer.setBackground(DARK_BG);
        menuContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- 1. Menu Header ---
        JPanel menuHeader = new JPanel(new BorderLayout(0, 10));
        menuHeader.setOpaque(false);

        JLabel headerLabel = new JLabel("FINE Resto - Menu");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerLabel.setForeground(TEXT_COLOR);
        menuHeader.add(headerLabel, BorderLayout.NORTH);

        // Kategori
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        categoryPanel.setBackground(DARK_BG);

        String[] categories = { "Dishes", "Drink", "Dessert" };
        for (String cat : categories) {
            JButton catBtn = createStyledButton(cat, DARK_BG, INACTIVE_TAB);
            if (cat.equals("Dishes")) {
                catBtn.setForeground(ACCENT_COLOR);
            }
            categoryPanel.add(catBtn);
        }
        menuHeader.add(categoryPanel, BorderLayout.CENTER);
        menuContainer.add(menuHeader, BorderLayout.NORTH);

        // --- 2. Menu List ---
        JPanel menuListPanel = new JPanel(new GridLayout(0, 3, 30, 30));
        menuListPanel.setBackground(DARK_BG);
        menuListPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        for (MenuItem item : menuItems) {
            menuListPanel.add(createMenuItemCard(item));
        }

        JScrollPane scrollPane = new JScrollPane(menuListPanel);
        scrollPane.getViewport().setBackground(DARK_BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUI(new ModernScrollBarUI());
        verticalBar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        verticalBar.setBackground(DARK_BG);
        verticalBar.setUnitIncrement(16);

        menuContainer.add(scrollPane, BorderLayout.CENTER);

        return menuContainer;
    }

    private JPanel createMenuItemCard(MenuItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SIDEBAR_BG);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setPreferredSize(new Dimension(200, 300));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Image Label
        JLabel imageLabel = new JLabel() {
            {
                setPreferredSize(new Dimension(150, 150));
                setMinimumSize(new Dimension(150, 150));
                setMaximumSize(new Dimension(150, 150));
                setOpaque(true);
                setBackground(DARK_BG);
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(TEXT_COLOR);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setClip(new Ellipse2D.Float(x, y, size, size));
                super.paintComponent(g2);
                g2.dispose();
            }
        };

        try {
            BufferedImage originalImage = ImageIO.read(new File(item.imagePath));
            if (originalImage != null) {
                int size = 150;
                Image scaledImage = originalImage.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.setText("");
            } else {
                imageLabel.setText("[Image Not Found]");
            }
        } catch (Exception e) {
            imageLabel.setText("[Image Load Error]");
            // System.err.println("Error memuat gambar: " + item.imagePath + " - " +
            // e.getMessage());
        }

        JPanel imageWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        imageWrapper.setOpaque(false);
        imageWrapper.add(imageLabel);

        // Nama
        JLabel nameLabel = new JLabel("<html><center>" + item.name + "</center></html>");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Harga
        JLabel priceLabel = new JLabel("$" + String.format("%.2f", item.price));
        priceLabel.setForeground(TEXT_COLOR);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameLabel.getPreferredSize().height));

        JPanel nameWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        nameWrapper.setOpaque(false);
        nameWrapper.add(nameLabel);

        card.add(imageWrapper);
        card.add(Box.createVerticalStrut(10));
        card.add(nameWrapper);
        card.add(Box.createVerticalStrut(5));
        card.add(priceLabel);

        // Efek Hover dan Click
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cartItems.put(item, cartItems.getOrDefault(item, 0) + 1);
                refreshCartPanelDisplay();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CART_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(SIDEBAR_BG);
            }
        });

        return card;
    }

    private JPanel createCartPanel() {
        JPanel cartContainer = new JPanel(new BorderLayout());
        cartContainer.setBackground(MEDIUM_BG);
        cartContainer.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- 1. Header Keranjang ---
        JLabel orderHeader = new JLabel("YOUR ORDER");
        orderHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        orderHeader.setForeground(TEXT_COLOR);

        // Tombol Lihat Antrian
        JButton listQueueBtn = createStyledButton("Lihat Antrian", DARK_BG, INACTIVE_TAB);
        listQueueBtn.addActionListener(e -> lihatAntrian());

        JPanel topCart = new JPanel(new BorderLayout(0, 5));
        topCart.setOpaque(false);
        topCart.add(orderHeader, BorderLayout.NORTH);
        topCart.add(listQueueBtn, BorderLayout.CENTER);

        cartContainer.add(topCart, BorderLayout.NORTH);

        // --- 2. Item Keranjang (Scrollable) ---
        cartItemsPanel = new JPanel(new BorderLayout());
        cartItemsPanel.setOpaque(true);
        cartItemsPanel.setBackground(MEDIUM_BG);

        itemsContainer = new JPanel();
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        itemsContainer.setOpaque(false);
        itemsContainer.setAlignmentY(Component.TOP_ALIGNMENT);

        JScrollPane itemScrollPane = new JScrollPane(itemsContainer);
        itemScrollPane.setBorder(BorderFactory.createEmptyBorder());
        itemScrollPane.getViewport().setBackground(MEDIUM_BG);
        itemScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        itemScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());

        cartItemsPanel.add(itemScrollPane, BorderLayout.CENTER);
        cartContainer.add(cartItemsPanel, BorderLayout.CENTER);

        // --- 3. Footer Keranjang ---
        JPanel footerPanel = createCartFooter();

        // Kontrol Debug & Exit
        JButton exitBtn = new JButton("Exit App");
        exitBtn.addActionListener(e -> exitApplication());
        exitBtn.setBackground(ACCENT_COLOR.darker());
        exitBtn.setForeground(TEXT_COLOR);

        // Grouping
        JPanel bottomControl = new JPanel(new BorderLayout(0, 5));
        bottomControl.setOpaque(false);
        bottomControl.add(new JLabel("Customer: ") {
            {
                setForeground(INACTIVE_TAB);
            }
        }, BorderLayout.WEST);
        bottomControl.add(dummyNameField, BorderLayout.CENTER);
        bottomControl.add(exitBtn, BorderLayout.EAST);

        JPanel finalSouthPanel = new JPanel(new BorderLayout(0, 10));
        finalSouthPanel.setOpaque(false);
        finalSouthPanel.add(footerPanel, BorderLayout.NORTH);
        finalSouthPanel.add(bottomControl, BorderLayout.SOUTH);

        cartContainer.add(finalSouthPanel, BorderLayout.SOUTH);

        refreshCartPanelDisplay();
        return cartContainer;
    }

    private JPanel createCartFooter() {
        JPanel footerPanel = new JPanel(new BorderLayout(0, 10));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel summaryPanel = new JPanel(new GridLayout(2, 2, 5, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.add(new JLabel("Total") {
            {
                setForeground(INACTIVE_TAB);
            }
        });

        subTotalLabel = new JLabel("$0.00");
        subTotalLabel.setHorizontalAlignment(SwingConstants.LEFT);
        subTotalLabel.setForeground(TEXT_COLOR);
        subTotalLabel.setFont(subTotalLabel.getFont().deriveFont(Font.BOLD));
        summaryPanel.add(subTotalLabel);

        JButton paymentBtn = createStyledButton("Continue to Payment", ACCENT_COLOR, TEXT_COLOR);
        paymentBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        paymentBtn.setPreferredSize(new Dimension(300, 50));
        paymentBtn.setBorder(null);

        paymentBtn.addActionListener(e -> processOrder());

        footerPanel.add(summaryPanel, BorderLayout.NORTH);
        footerPanel.add(paymentBtn, BorderLayout.SOUTH);

        return footerPanel;
    }

    private void refreshCartPanelDisplay() {
        itemsContainer.removeAll();
        double total = 0;
        Component oldHeader = ((BorderLayout) cartItemsPanel.getLayout()).getLayoutComponent(cartItemsPanel,
                BorderLayout.NORTH);
        if (oldHeader != null) {
            cartItemsPanel.remove(oldHeader);
        }

        JPanel headerPanel = createCartColumnHeader();
        cartItemsPanel.add(headerPanel, BorderLayout.NORTH);

        for (Map.Entry<MenuItem, Integer> entry : cartItems.entrySet()) {
            MenuItem item = entry.getKey();
            Integer qty = entry.getValue();
            itemsContainer.add(createCartItem(item, qty));
            itemsContainer.add(Box.createVerticalStrut(5));
            total += item.price * qty;
        }

        if (subTotalLabel != null) {
            subTotalLabel.setText(String.format("$%.2f", total));
        }

        itemsContainer.revalidate();
        itemsContainer.repaint();
        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JPanel createCartColumnHeader() {
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setOpaque(true);
        headerPanel.setBackground(MEDIUM_BG);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, INACTIVE_TAB.darker()));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 3, 5, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font headerFont = new Font("SansSerif", Font.BOLD, 10);

        // Kolom ITEM (Berat 0.6)
        JLabel itemHeader = new JLabel("ITEM");
        itemHeader.setForeground(INACTIVE_TAB);
        itemHeader.setFont(headerFont);
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.6;
        headerPanel.add(itemHeader, gbc);

        // Kolom QTY (Berat 0.2)
        JLabel qtyHeader = new JLabel("QTY");
        qtyHeader.setForeground(INACTIVE_TAB);
        qtyHeader.setFont(headerFont);
        qtyHeader.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.2;
        headerPanel.add(qtyHeader, gbc);

        // Kolom PRICE (Berat 0.1)
        JLabel priceHeader = new JLabel("PRICE");
        priceHeader.setForeground(INACTIVE_TAB);
        priceHeader.setFont(headerFont);
        priceHeader.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.15;
        headerPanel.add(priceHeader, gbc);

        // Kolom HAPUS (Berat 0.05)
        JLabel deleteHeader = new JLabel("");
        gbc.gridx = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0.05;
        headerPanel.add(deleteHeader, gbc);

        return headerPanel;
    }

    private JPanel createCartItem(MenuItem item, int qty) {
        JPanel itemPanel = new JPanel(new GridBagLayout());
        itemPanel.setOpaque(true);
        itemPanel.setBackground(MEDIUM_BG);
        itemPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DARK_BG));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 3, 5, 3);

        double itemTotal = item.price * qty;

        // --- Kolom 0: Item Name ---
        JTextArea nameArea = new JTextArea(item.name);
        nameArea.setPreferredSize(new Dimension(150, 40));
        nameArea.setWrapStyleWord(true);
        nameArea.setLineWrap(true);
        nameArea.setForeground(TEXT_COLOR);
        nameArea.setBackground(MEDIUM_BG);
        nameArea.setEditable(false);
        nameArea.setBorder(null);
        nameArea.setFont(new Font("SansSerif", Font.PLAIN, 12));

        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        itemPanel.add(nameArea, gbc);

        // --- Kolom 1-3: Quantity Group ---
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        qtyPanel.setOpaque(false);

        JButton minusBtn = createQtyButton("-", DARK_BG, item, -1);
        JTextField qtyField = new JTextField(String.valueOf(qty));
        qtyField.setPreferredSize(new Dimension(40, 30));
        qtyField.setHorizontalAlignment(SwingConstants.CENTER);
        qtyField.setBackground(ACCENT_COLOR.darker());
        qtyField.setForeground(TEXT_COLOR);
        qtyField.setEditable(false);
        qtyField.setBorder(null);
        JButton plusBtn = createQtyButton("+", DARK_BG, item, 1);

        qtyPanel.add(minusBtn);
        qtyPanel.add(qtyField);
        qtyPanel.add(plusBtn);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        itemPanel.add(qtyPanel, gbc);

        // --- Kolom 4: Total Price ---
        JLabel totalLabel = new JLabel("$" + String.format("%.2f", itemTotal));
        totalLabel.setForeground(TEXT_COLOR);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.15;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        itemPanel.add(totalLabel, gbc);

        // --- Kolom 5: Delete Button ---
        JButton deleteBtn = new JButton("🗑️");
        deleteBtn.setBackground(MEDIUM_BG);
        deleteBtn.setForeground(ACCENT_COLOR);
        deleteBtn.setPreferredSize(new Dimension(30, 30));
        deleteBtn.setBorder(null);

        deleteBtn.addActionListener(e -> {
            cartItems.remove(item);
            refreshCartPanelDisplay();
        });

        gbc.gridx = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0.05;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        itemPanel.add(deleteBtn, gbc);

        return itemPanel;
    }

    // Helper method untuk tombol +/-
    private JButton createQtyButton(String text, Color bg, MenuItem item, int change) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(TEXT_COLOR);
        btn.setPreferredSize(new Dimension(30, 30));
        btn.setBorder(null);

        btn.addActionListener(e -> {
            int currentQty = cartItems.get(item);
            if (change == -1) {
                if (currentQty > 1) {
                    cartItems.put(item, currentQty - 1);
                } else {
                    cartItems.remove(item);
                }
            } else if (change == 1) {
                cartItems.put(item, currentQty + 1);
            }
            refreshCartPanelDisplay();
        });
        return btn;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        return btn;
    }

    // =======================================================
    // --- CLASS CUSTOM SCROLLBAR ---
    // =======================================================
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        private final Dimension d = new Dimension();

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d;
                }
            };
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d;
                }
            };
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color color = null;
            JScrollBar sb = (JScrollBar) c;
            if (!sb.isEnabled() || r.width > r.height) {
                return;
            }
            if (isDragging) {
                color = new Color(133, 137, 153, 200);
            } else if (isThumbRollover()) {
                color = new Color(133, 137, 153, 150);
            } else {
                color = new Color(133, 137, 153, 100);
            }

            g2.setPaint(color);
            g2.fillRoundRect(r.x + 4, r.y, r.width - 8, r.height, 10, 10);
            g2.dispose();
        }

        @Override
        protected void setThumbBounds(int x, int y, int width, int height) {
            super.setThumbBounds(x, y, width, height);
            scrollbar.repaint();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Fallback
        }

        SwingUtilities.invokeLater(() -> {
            new GUI().setVisible(true);
        });
    }
}