
import server.Order; // Pastikan Order sudah diimplementasikan dan Serializable
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

public class RestoStaffGUI extends JFrame {

    // --- VARIABEL KONEKSI ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream objectIn;

    // --- SKEMA WARNA ---
    private final Color DARK_BG = new Color(30, 31, 37);
    private final Color TEXT_COLOR = new Color(255, 255, 255);
    private final Color ACCENT_COLOR = new Color(107, 255, 107);
    private final Color MEDIUM_BG = new Color(48, 51, 61);
    private final Color HEADER_COLOR = new Color(20, 20, 24);

    // --- KOMPONEN GUI ---
    private JTextArea statusArea;
    private JTable orderTable;
    private DefaultTableModel tableModel;

    public RestoStaffGUI() {
        setTitle("RESTO STAFF CONTROL - SmartQueue Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(DARK_BG);
        setLayout(new BorderLayout(10, 10));

        // --- 2. Panel Tengah (Order Report/Antrian) ---
        add(createOrderReportPanel(), BorderLayout.CENTER);

        // --- 3. Status Koneksi (Bawah) ---
        statusArea = new JTextArea(3, 20);
        statusArea.setEditable(false);
        statusArea.setBackground(DARK_BG.darker());
        statusArea.setForeground(TEXT_COLOR);
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        setVisible(true);
        new Thread(this::setupConnection).start();
    }

    // =======================================================
    // --- UI COMPONENTS ---
    // =======================================================

    private JPanel createOrderReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(new EmptyBorder(0, 10, 0, 10));
        panel.setBackground(DARK_BG);

        // Header Laporan
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_BG);
        JLabel title = new JLabel("Order Report");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT_COLOR);
        header.add(title, BorderLayout.WEST);

        // Tombol Proses/Refresh
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));
        actionPanel.setBackground(DARK_BG);

        JButton processBtn = new JButton("Selesaikan Pesanan Pertama");
        processBtn.setBackground(new Color(255, 107, 107));
        processBtn.setForeground(TEXT_COLOR);
        processBtn.addActionListener(e -> processDequeue());
        actionPanel.add(processBtn);

        JButton refreshBtn = new JButton("Refresh Antrian");
        refreshBtn.setBackground(ACCENT_COLOR.darker());
        refreshBtn.setForeground(TEXT_COLOR);
        refreshBtn.addActionListener(e -> lihatAntrian());
        actionPanel.add(refreshBtn);

        header.add(actionPanel, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Tabel Antrian
        String[] columnNames = { "No. Antrian", "Pelanggan", "Menu Dipesan", "Status" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable = new JTable(tableModel);

        orderTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        orderTable.setRowHeight(30);
        orderTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        orderTable.getTableHeader().setBackground(HEADER_COLOR.darker());
        orderTable.getTableHeader().setForeground(TEXT_COLOR);
        orderTable.setBackground(MEDIUM_BG);
        orderTable.setForeground(TEXT_COLOR);
        orderTable.setSelectionBackground(ACCENT_COLOR.darker());
        orderTable.setGridColor(MEDIUM_BG);

        // --- RENDERER TEMA GELAP DEFAULT ---
        orderTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    // Hanya kolom Menu Dipesan yang akan diganti di bawah
                    if (column != 2) {
                        c.setBackground(MEDIUM_BG);
                        c.setForeground(TEXT_COLOR);
                    }
                }
                return c;
            }
        });

        javax.swing.table.TableColumnModel columnModel = orderTable.getColumnModel();

        // PENGATURAN LEBAR KOLOM
        columnModel.getColumn(0).setPreferredWidth(80);
        columnModel.getColumn(0).setMaxWidth(50);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(450);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(3).setMaxWidth(150);

        // --- Terapkan WRAPPING TEXT Renderer pada Kolom "Menu Dipesan" (Index 2) ---
        columnModel.getColumn(2).setCellRenderer(new TextAreaRenderer());

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.getViewport().setBackground(DARK_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // =======================================================
    // --- NETWORKING & THREADING (Gunakan sendCommand) ---
    // =======================================================

    private Object sendCommand(String command) throws Exception {
        synchronized (socket) {
            out.writeObject(command);
            out.flush();
            return objectIn.readObject();
        }
    }

    private void setupConnection() {
        SwingUtilities.invokeLater(() -> statusArea.append("Mencoba koneksi ke server...\n"));
        try {
            socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            objectIn = new ObjectInputStream(socket.getInputStream());

            Object welcome = objectIn.readObject();
            if (welcome instanceof String) {
                SwingUtilities.invokeLater(() -> statusArea.append("Status: " + (String) welcome + "\n"));
            }

            SwingUtilities.invokeLater(this::lihatAntrian);

        } catch (Exception e) {
            SwingUtilities
                    .invokeLater(() -> statusArea.append("Gagal koneksi ke server. Error: " + e.getMessage() + "\n"));
        }
    }

    private void lihatAntrian() {
        if (out == null || socket == null || socket.isClosed()) {
            SwingUtilities.invokeLater(
                    () -> JOptionPane.showMessageDialog(this, "Koneksi ke server belum siap atau terputus."));
            return;
        }

        new Thread(() -> {
            try {
                Object obj = sendCommand("LIST");

                @SuppressWarnings("unchecked")
                Queue<Order> orders = (Queue<Order>) obj;

                SwingUtilities.invokeLater(() -> updateQueueDisplay(orders));

            } catch (Exception e) {
                SwingUtilities.invokeLater(
                        () -> statusArea.append("Error saat meminta antrian (LIST): " + e.getMessage() + "\n"));
                e.printStackTrace();
            }
        }).start();
    }

    private void processDequeue() {
        if (out == null || socket.isClosed()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Koneksi terputus."));
            return;
        }

        new Thread(() -> {
            try {
                Object response = sendCommand("DEQUEUE");

                if (response instanceof String) {
                    SwingUtilities.invokeLater(() -> {
                        statusArea.append("Server: " + (String) response + "\n");
                        lihatAntrian();
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusArea.append("Error DEQUEUE: " + e.getMessage() + "\n"));
                e.printStackTrace();
            }
        }).start();
    }

    // =======================================================
    // --- UI DISPLAY DENGAN JTable ---
    // =======================================================

    private void updateQueueDisplay(Queue<Order> orders) {
        tableModel.setRowCount(0);
        if (orders != null && !orders.isEmpty()) {
            int counter = 0;
            for (Order order : orders) {
                String status = (counter == 0) ? "Preparing (Next to Process)" : "Pending";

                // --- PENGAMBILAN DATA (Gunakan try-catch jika getter tidak ada di Order.java)
                // ---
                String customerName = "N/A";
                String menuDetails = "N/A";

                try {
                    customerName = order.getCustomerName();
                } // Asumsi: Order memiliki getName()
                catch (Exception e) {
                    customerName = "Pelanggan " + order.getOrderNumber();
                }

                try {
                    menuDetails = order.getMenuItem();
                } // Asumsi: Order memiliki getMenu()
                catch (Exception e) {
                    menuDetails = order.toString();
                } // Fallback

                // --- AKHIR PENGAMBILAN DATA ---

                Object[] rowData = {
                        order.getOrderNumber(),
                        customerName,
                        menuDetails,
                        status
                };
                tableModel.addRow(rowData);

                counter++;
            }
            // Setelah semua data ditambahkan, instruksikan tabel untuk menghitung ulang
            // ukuran
            orderTable.repaint();
            orderTable.revalidate();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> new RestoStaffGUI().setVisible(true));
    }

    // =======================================================
    // --- CLASS RENDERER UNTUK WRAPPING TEXT ---
    // =======================================================
    class TextAreaRenderer extends JTextArea implements TableCellRenderer {

        public TextAreaRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            setText((value == null) ? "" : value.toString());

            // Atur warna sesuai tema
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(RestoStaffGUI.this.MEDIUM_BG);
                setForeground(RestoStaffGUI.this.TEXT_COLOR);
            }

            // --- LOGIKA MENGHITUNG TINGGI BARIS SECARA DINAMIS ---
            // Set lebar kolom agar JTextArea dapat menghitung ketinggian yang dibutuhkan
            int preferredWidth = table.getColumnModel().getColumn(column).getWidth();
            setSize(new Dimension(preferredWidth, 1)); // Tinggi dummy agar lebar dihitung

            int preferredHeight = (int) getPreferredSize().getHeight();

            // Dapatkan tinggi baris saat ini
            int rowHeight = table.getRowHeight(row);

            // Cek apakah tinggi yang dibutuhkan melebihi tinggi baris saat ini
            if (preferredHeight > rowHeight) {
                table.setRowHeight(row, preferredHeight);
            } else if (preferredHeight < rowHeight && rowHeight > 30) {
                // Opsional: Jika baris sangat tinggi dan sekarang teksnya pendek,
                // reset tinggi (hati-hati dengan performa)
                // table.setRowHeight(row, 30);
            }
            // --- AKHIR LOGIKA MENGHITUNG TINGGI BARIS ---

            return this;
        }
    }
}