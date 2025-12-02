import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.EOFException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import server.Order;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class GUI extends JFrame {

    // --- Variabel Socket ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream objectIn;
    // --- End Socket Variables ---

    private final Color DARK_BG = new Color(30, 31, 37);
    private final Color MEDIUM_BG = new Color(48, 51, 61);
    private final Color TEXT_COLOR = new Color(255, 255, 255);
    private final Color ACCENT_COLOR = new Color(255, 107, 107);
    private final Color INACTIVE_TAB = new Color(133, 137, 153);

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
    private JLabel subTotalLabel;

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

        // --- Setup UI Components ---
        add(createHeaderPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(800);
        splitPane.setDividerSize(5);
        splitPane.setBackground(DARK_BG);
        splitPane.setOpaque(false);

        splitPane.setLeftComponent(createMenuPanel());

        JPanel cartContainer = createCartPanel();
        splitPane.setRightComponent(cartContainer);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(new JScrollPane(connectionStatusArea), BorderLayout.CENTER);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setOpaque(false);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        mainContentPanel.add(splitPane, BorderLayout.CENTER);
        mainContentPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainContentPanel, BorderLayout.CENTER);

        setVisible(true);
        new Thread(() -> setupConnection()).start();
    }

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
            e.printStackTrace();
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
                Queue<Order> orders = (Queue<Order>) obj;

                SwingUtilities.invokeLater(() -> {
                    connectionStatusArea.append("===== Daftar Antrian Terbaru (" + orders.size() + ") =====\n");
                    if (orders.isEmpty()) {
                        connectionStatusArea.append("(Kosong)\n");
                    } else {
                        orders.forEach(order -> connectionStatusArea.append(order.toString() + "\n"));
                    }
                    connectionStatusArea.append("\n");
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
    // --- CLASS CUSTOM SCROLLBAR ---
    // =======================================================
    private static class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        private final Dimension d = new Dimension();

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d; // Ukuran 0 (Hilangkan tombol panah atas)
                }
            };
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return new JButton() {
                @Override
                public Dimension getPreferredSize() {
                    return d; // Ukuran 0 (Hilangkan tombol panah bawah)
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
            } else if (isDragging) {
                color = new Color(133, 137, 153, 200);
            } else if (isThumbRollover()) {
                color = new Color(133, 137, 153, 150);
            } else {
                color = new Color(133, 137, 153, 100);

                g2.setPaint(color);
                g2.fillRoundRect(r.x + 4, r.y, r.width - 8, r.height, 10, 10);
                g2.dispose();
            }
        }

        @Override
        protected void setThumbBounds(int x, int y, int width, int height) {
            super.setThumbBounds(x, y, width, height);
            scrollbar.repaint();
        }
    }

    // =======================================================
    // --- LOGIKA UI MODERN ---
    // =======================================================
    private List<MenuItem> menuItems = new ArrayList<>(Arrays.asList(
            new MenuItem("Spicy seafood noodles", 2.29,
                    "D:\\Pemrograman Internet\\SmartQueue\\images\\thai-food-tom-yum-kung-river-prawn-spicy-soup.png"),
            new MenuItem("Salted Pasta w/ mushroom sauce", 2.69, "images/pasta.jpg"),
            new MenuItem("Beef dumpling in hot and sour soup", 2.99, "images/dumpling.jpg"),
            new MenuItem("Healthy noodle w/ spinach leaf", 3.29, "images/spinach_noodle.jpg"),
            new MenuItem("Hot spicy fried rice w/ omelet", 3.49, "images/fried_rice.jpg"),
            new MenuItem("Spicy instant noodle", 3.59, "images/instant_noodle.jpg")));

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(DARK_BG);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel logoLabel = new JLabel("FINE Resto");
        logoLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        logoLabel.setForeground(TEXT_COLOR);

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setOpaque(false);
        leftHeader.add(logoLabel, BorderLayout.NORTH);

        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerHeader.setOpaque(false);

        headerPanel.add(leftHeader, BorderLayout.WEST);
        headerPanel.add(centerHeader, BorderLayout.CENTER);

        headerPanel.add(new JSeparator(SwingConstants.HORIZONTAL) {
            {
                setForeground(MEDIUM_BG);
            }
        }, BorderLayout.SOUTH);

        return headerPanel;
    }

    private JPanel createMenuPanel() {
        JPanel menuContainer = new JPanel(new BorderLayout());
        menuContainer.setBackground(DARK_BG);
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        categoryPanel.setBackground(DARK_BG);

        String[] categories = { "Dishes", "Drink", "Dessert" };
        for (String cat : categories) {
            JButton catBtn = createStyledButton(cat, DARK_BG, INACTIVE_TAB);
            if (cat.equals("Dishes")) {
                catBtn.setForeground(ACCENT_COLOR);
            }
            categoryPanel.add(catBtn);
        }

        JPanel topMenuControl = new JPanel(new BorderLayout());
        topMenuControl.setOpaque(false);
        topMenuControl.add(new JLabel("Choose Dishes") {
            {
                setFont(new Font("SansSerif", Font.BOLD, 18));
                setForeground(TEXT_COLOR);
                setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            }
        }, BorderLayout.WEST);

        JPanel sortWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sortWrapper.setOpaque(false);
        topMenuControl.add(sortWrapper, BorderLayout.EAST);

        menuContainer.add(categoryPanel, BorderLayout.NORTH);

        JPanel menuListPanel = new JPanel(new GridLayout(0, 3, 30, 30));
        menuListPanel.setBackground(DARK_BG);
        menuListPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        for (MenuItem item : menuItems) {
            menuListPanel.add(createMenuItemCard(item));
        }

        JScrollPane scrollPane = new JScrollPane(menuListPanel);
        scrollPane.getViewport().setBackground(DARK_BG);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUI(new ModernScrollBarUI()); // Pasang UI baru
        verticalBar.setPreferredSize(new Dimension(15, Integer.MAX_VALUE));
        verticalBar.setBackground(DARK_BG);
        verticalBar.setUnitIncrement(16);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(topMenuControl, BorderLayout.NORTH);
        contentWrapper.add(scrollPane, BorderLayout.CENTER);

        menuContainer.add(contentWrapper, BorderLayout.CENTER);

        return menuContainer;
    }

    private JPanel createMenuItemCard(MenuItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(MEDIUM_BG);
        card.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));
        card.setPreferredSize(new Dimension(100, 100));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- START: MODIFIKASI MUAT GAMBAR ---
        JLabel imageLabel = new JLabel() {
            {
                setPreferredSize(new Dimension(180, 200));
                setOpaque(true);
                setBackground(MEDIUM_BG);
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(TEXT_COLOR);
            }
        };

        try {
            // Membaca file gambar
            BufferedImage originalImage = ImageIO.read(new File(item.imagePath));
            if (originalImage != null) {
                // Mengubah ukuran gambar agar sesuai dengan JLabel (180x120)
                Image scaledImage = originalImage.getScaledInstance(180, 120, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
                imageLabel.setText(""); // Hapus placeholder "[Image]"
            } else {
                imageLabel.setText("[Image Not Found]");
            }
        } catch (Exception e) {
            imageLabel.setText("[Image Load Error]");
            System.err.println("Error memuat gambar: " + item.imagePath + " - " + e.getMessage());
        }
        // --- END: MODIFIKASI MUAT GAMBAR ---

        JPanel imageWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        imageWrapper.setOpaque(false);
        imageWrapper.add(imageLabel);

        JLabel nameLabel = new JLabel(item.name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel("$" + String.format("%.2f", item.price));
        priceLabel.setForeground(TEXT_COLOR);
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(imageWrapper);
        card.add(Box.createVerticalStrut(5));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(priceLabel);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                cartItems.put(item, cartItems.getOrDefault(item, 0) + 1);
                refreshCartPanelDisplay();
            }
        });

        return card;
    }

    private JPanel createCartPanel() {
        JPanel cartContainer = new JPanel(new BorderLayout());
        cartContainer.setBackground(MEDIUM_BG);
        cartContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel orderHeader = new JLabel("YOUR ORDER");
        orderHeader.setFont(new Font("SansSerif", Font.BOLD, 20));
        orderHeader.setForeground(TEXT_COLOR);

        JPanel cartNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        cartNav.setBackground(MEDIUM_BG);

        JPanel topCart = new JPanel(new GridLayout(2, 1));
        topCart.setOpaque(false);
        topCart.add(orderHeader);
        topCart.add(cartNav);

        cartItemsPanel = new JPanel();
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));
        cartItemsPanel.setOpaque(false);

        JScrollPane itemScrollPane = new JScrollPane(cartItemsPanel);
        itemScrollPane.setBorder(BorderFactory.createEmptyBorder());
        itemScrollPane.getViewport().setBackground(MEDIUM_BG);
        itemScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel footerPanel = createCartFooter();

        cartContainer.add(topCart, BorderLayout.NORTH);
        cartContainer.add(itemScrollPane, BorderLayout.CENTER);

        refreshCartPanelDisplay();

        JButton listQueueBtn = new JButton("Lihat Antrian Server");
        listQueueBtn.addActionListener(e -> lihatAntrian());
        listQueueBtn.setBackground(MEDIUM_BG.brighter());
        listQueueBtn.setForeground(TEXT_COLOR);

        JButton exitBtn = new JButton("Exit");
        exitBtn.addActionListener(e -> exitApplication());
        exitBtn.setBackground(MEDIUM_BG.brighter());
        exitBtn.setForeground(TEXT_COLOR);

        JPanel debugPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        debugPanel.setOpaque(false);
        debugPanel.add(listQueueBtn);
        debugPanel.add(Box.createHorizontalStrut(10));
        debugPanel.add(exitBtn);

        JPanel bottomControl = new JPanel(new BorderLayout(0, 5));
        bottomControl.setOpaque(false);
        bottomControl.add(dummyNameField, BorderLayout.NORTH);
        bottomControl.add(debugPanel, BorderLayout.SOUTH);

        JPanel finalSouthPanel = new JPanel(new BorderLayout(0, 10));
        finalSouthPanel.setOpaque(false);
        finalSouthPanel.add(footerPanel, BorderLayout.NORTH);
        finalSouthPanel.add(bottomControl, BorderLayout.SOUTH);

        cartContainer.remove(footerPanel);
        cartContainer.add(finalSouthPanel, BorderLayout.SOUTH);

        refreshCartPanelDisplay();

        return cartContainer;
    }

    private JPanel createCartFooter() {
        // ... (Kode Cart Footer tidak berubah) ...
        JPanel footerPanel = new JPanel(new BorderLayout(0, 10));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

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
        paymentBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        paymentBtn.addActionListener(e -> processOrder());

        footerPanel.add(summaryPanel, BorderLayout.NORTH);
        footerPanel.add(paymentBtn, BorderLayout.SOUTH);

        return footerPanel;
    }

    private void refreshCartPanelDisplay() {
        cartItemsPanel.removeAll();
        double total = 0;

        JPanel header = new JPanel(new GridLayout(1, 6)); // Ditambah 1 kolom (Minus)
        header.setOpaque(false);
        header.add(new JLabel("Item") {
            {
                setForeground(INACTIVE_TAB);
            }
        });
        header.add(new JLabel("") {
            {
                setForeground(INACTIVE_TAB);
            }
        }); // Kolom Minus
        header.add(new JLabel("Qty") {
            {
                setForeground(INACTIVE_TAB);
                setHorizontalAlignment(SwingConstants.CENTER);
            }
        });
        header.add(new JLabel("") {
            {
                setForeground(INACTIVE_TAB);
            }
        }); // Kolom Plus
        header.add(new JLabel("Price") {
            {
                setForeground(INACTIVE_TAB);
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        });
        header.add(new JLabel("") {
            {
                setForeground(INACTIVE_TAB);
            }
        });
        cartItemsPanel.add(header);
        cartItemsPanel.add(new JSeparator() {
            {
                setForeground(DARK_BG.brighter());
            }
        });

        for (Map.Entry<MenuItem, Integer> entry : cartItems.entrySet()) {
            MenuItem item = entry.getKey();
            Integer qty = entry.getValue();
            cartItemsPanel.add(createCartItem(item, qty));
            total += item.price * qty;
        }

        if (subTotalLabel != null) {
            subTotalLabel.setText(String.format("$%.2f", total));
        }
        cartItemsPanel.revalidate();
        cartItemsPanel.repaint();
    }

    private JPanel createCartItem(MenuItem item, int qty) {
        // ... (Kode createCartItem tidak berubah) ...
        JPanel itemPanel = new JPanel(new GridBagLayout());
        itemPanel.setOpaque(false);
        itemPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 3, 0, 3);

        double itemTotal = item.price * qty;

        // Name
        JLabel nameLabel = new JLabel(item.name);
        nameLabel.setForeground(TEXT_COLOR);

        JPanel nameWrapper = new JPanel(new BorderLayout());
        nameWrapper.setOpaque(false);
        nameWrapper.add(nameLabel, BorderLayout.NORTH);

        // Tombol MINUS (-)
        JButton minusBtn = new JButton("-");
        minusBtn.setBackground(DARK_BG);
        minusBtn.setForeground(TEXT_COLOR);
        minusBtn.setPreferredSize(new Dimension(30, 30));
        minusBtn.setBorder(null);

        minusBtn.addActionListener(e -> {
            int currentQty = cartItems.get(item);
            if (currentQty > 1) {
                cartItems.put(item, currentQty - 1);
            } else {
                cartItems.remove(item);
            }
            refreshCartPanelDisplay();
        });

        // Quantity Field
        JTextField qtyField = new JTextField(String.valueOf(qty));
        qtyField.setPreferredSize(new Dimension(30, 30));
        qtyField.setHorizontalAlignment(SwingConstants.CENTER);
        qtyField.setBackground(ACCENT_COLOR.darker());
        qtyField.setForeground(TEXT_COLOR);
        qtyField.setEditable(false);

        // Tombol PLUS (+)
        JButton plusBtn = new JButton("+");
        plusBtn.setBackground(MEDIUM_BG);
        plusBtn.setForeground(TEXT_COLOR);
        plusBtn.setPreferredSize(new Dimension(30, 30));
        plusBtn.setBorder(null);

        plusBtn.addActionListener(e -> {
            cartItems.put(item, cartItems.get(item) + 1);
            refreshCartPanelDisplay();
        });

        // Total Price
        JLabel totalLabel = new JLabel("$" + String.format("%.2f", itemTotal));
        totalLabel.setForeground(TEXT_COLOR);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // Delete button
        JButton deleteBtn = new JButton("🗑️");
        deleteBtn.setBackground(MEDIUM_BG);
        deleteBtn.setForeground(ACCENT_COLOR);
        deleteBtn.setPreferredSize(new Dimension(30, 30));
        deleteBtn.setBorder(null);

        deleteBtn.addActionListener(e -> {
            cartItems.remove(item);
            refreshCartPanelDisplay();
        });

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        itemPanel.add(nameWrapper, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        itemPanel.add(minusBtn, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        itemPanel.add(qtyField, gbc);
        gbc.gridx = 3;
        gbc.weightx = 0;
        itemPanel.add(plusBtn, gbc);
        gbc.gridx = 4;
        gbc.weightx = 0;
        itemPanel.add(totalLabel, gbc);
        gbc.gridx = 5;
        gbc.weightx = 0;
        itemPanel.add(deleteBtn, gbc);

        return itemPanel;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        return btn;
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