import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

class MemoryBlock {
    int size;
    boolean allocated;
    int processId;

    MemoryBlock(int size) {
        this.size = size;
        this.allocated = false;
        this.processId = -1;
    }

    public String toString() {
        return "Size: " + size + " | Allocated: " + allocated + " | PID: " + (processId == -1 ? "None" : processId);
    }
}

class MemoryManager {
    List<MemoryBlock> memory;
    ReentrantLock lock = new ReentrantLock();
    JTextArea logArea;
    DefaultTableModel tableModel;

    MemoryManager(JTextArea logArea, DefaultTableModel tableModel) {
        this.memory = new ArrayList<>();
        this.logArea = logArea;
        this.tableModel = tableModel;
    }

    boolean allocate(int pid, int size, String strategy) {
        lock.lock();
        try {
            int index = -1;
            switch (strategy.toLowerCase()) {
                case "first":
                    for (int i = 0; i < memory.size(); i++) {
                        if (!memory.get(i).allocated && memory.get(i).size >= size) {
                            index = i;
                            break;
                        }
                    }
                    break;
                case "best":
                    int bestFit = Integer.MAX_VALUE;
                    for (int i = 0; i < memory.size(); i++) {
                        MemoryBlock block = memory.get(i);
                        if (!block.allocated && block.size >= size && block.size < bestFit) {
                            bestFit = block.size;
                            index = i;
                        }
                    }
                    break;
                case "worst":
                    int worstFit = -1;
                    for (int i = 0; i < memory.size(); i++) {
                        MemoryBlock block = memory.get(i);
                        if (!block.allocated && block.size >= size && block.size > worstFit) {
                            worstFit = block.size;
                            index = i;
                        }
                    }
                    break;
            }

            if (index != -1) {
                memory.get(index).allocated = true;
                memory.get(index).processId = pid;
                log("Allocated PID " + pid + " to block " + (index + 1) + " using " + strategy + " fit.");
                updateTable();
                return true;
            } else {
                log("Failed to allocate memory for PID " + pid);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    void deallocate(int pid) {
        lock.lock();
        try {
            for (MemoryBlock block : memory) {
                if (block.processId == pid) {
                    log("Deallocated PID " + pid + " from block of size " + block.size);
                    block.allocated = false;
                    block.processId = -1;
                }
            }
            updateTable();
        } finally {
            lock.unlock();
        }
    }

    void updateTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < memory.size(); i++) {
            MemoryBlock block = memory.get(i);
            tableModel.addRow(new Object[]{i + 1, block.size, block.allocated ? "Yes" : "No", block.processId == -1 ? "" : block.processId});
        }
    }

    void log(String msg) {
        logArea.append(msg + "\n");
    }

    void initializeMemory() {
        memory.clear();
        Random rand = new Random();

        for (int i = 0; i < 5; i++) {
            String input = JOptionPane.showInputDialog("Enter size for Memory Block " + (i + 1) + ":");
            try {
                int baseSize = Integer.parseInt(input);
                // Adding randomness to make the size near the input size
                int adjustment = rand.nextInt(100) - 50; // Adjusting within -50 to +50
                int size = Math.max(baseSize + adjustment, 50); // Ensure size is at least 50

                memory.add(new MemoryBlock(size));
            } catch (NumberFormatException e) {
                log("Invalid size input. Using default size 100 for block " + (i + 1));
                memory.add(new MemoryBlock(100));
            }
        }

        updateTable();
        log("Initialized memory blocks with random sizes near the user input.\nYou can now allocate.");
    }
}

public class AdvancedMemorySimulator extends JFrame {
    JTextArea logArea = new JTextArea();
    JTextField pidField = new JTextField();
    JTextField sizeField = new JTextField();
    JComboBox<String> strategyBox = new JComboBox<>(new String[]{"First", "Best", "Worst"});
    JTable memoryTable;
    DefaultTableModel tableModel;
    MemoryManager manager;

    public AdvancedMemorySimulator() {
        setTitle("Advanced OS Memory Manager Simulator");
        setSize(800, 600);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        logArea.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(logArea);

        tableModel = new DefaultTableModel(new Object[]{"Block #", "Size", "Allocated", "PID"}, 0);
        memoryTable = new JTable(tableModel);
        JScrollPane scrollTable = new JScrollPane(memoryTable);

        manager = new MemoryManager(logArea, tableModel);

        JPanel inputPanel = new JPanel(new GridLayout(3, 4, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Memory Operations"));

        inputPanel.add(new JLabel("PID:"));
        inputPanel.add(pidField);
        inputPanel.add(new JLabel("Memory Size:"));
        inputPanel.add(sizeField);

        inputPanel.add(new JLabel("Allocation Strategy:"));
        inputPanel.add(strategyBox);

        JButton allocBtn = new JButton("Allocate");
        JButton deallocBtn = new JButton("Deallocate");
        JButton initBtn = new JButton("Initialize Memory");
        inputPanel.add(allocBtn);
        inputPanel.add(deallocBtn);
        inputPanel.add(initBtn);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollLog, scrollTable);
        splitPane.setDividerLocation(250);

        add(inputPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        initBtn.addActionListener(e -> {
            manager.initializeMemory();
        });

        allocBtn.addActionListener(e -> {
            try {
                int pid = Integer.parseInt(pidField.getText());
                int size = Integer.parseInt(sizeField.getText());
                String strategy = strategyBox.getSelectedItem().toString().toLowerCase();

                manager.allocate(pid, size, strategy);

            } catch (NumberFormatException ex) {
                logArea.append("Invalid input!\n");
            }
        });

        deallocBtn.addActionListener(e -> {
            try {
                int pid = Integer.parseInt(pidField.getText());
                manager.deallocate(pid);
            } catch (NumberFormatException ex) {
                logArea.append("Invalid PID!\n");
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdvancedMemorySimulator::new);
    }
}