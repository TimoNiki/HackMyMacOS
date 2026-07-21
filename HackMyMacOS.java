import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HackMyMacOS {

    private static ScheduledExecutorService notificationScheduler;
    private static boolean isSpammingNotifications = false;

    private static ScheduledExecutorService speechScheduler;
    private static boolean isSpammingSpeech = false;

    private static JTextArea logArea;
    private static Process logProcess;
    private static boolean isReadingLogs = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Hack my macOS 1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 450);
        frame.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Основные", createMainTab());
        tabbedPane.addTab("Веселье и тесты", createFunTab());
        tabbedPane.addTab("Логи macOS", createLogsTab());
        tabbedPane.addTab("About", createAboutTab());

        frame.add(tabbedPane);
        frame.setVisible(true);

        startStreamingMacLogs();
    }

    private static JPanel createMainTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));

        JLabel titleLabel = new JLabel("Основные настройки системы");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));

        Dimension btnSize = new Dimension(340, 40);

        JButton themeButton = new JButton("Переключить тему (Светлая/Темная)");
        themeButton.setFont(new Font("Arial", Font.PLAIN, 13));
        themeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        themeButton.setMaximumSize(btnSize);
        themeButton.addActionListener(e -> {
            String toggleThemeScript = "tell application \"System Events\" to tell appearance preferences to set dark mode to not dark mode";
            runAppleScript(toggleThemeScript);
        });
        panel.add(themeButton);
        
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        JLabel futureLabel = new JLabel("Здесь появятся новые системные утилиты...");
        futureLabel.setForeground(Color.GRAY);
        futureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(futureLabel);

        return panel;
    }

    private static JPanel createFunTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Dimension btnSize = new Dimension(340, 35);


        JButton spamButton = new JButton("Включить спам-уведомления");
        spamButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        spamButton.setMaximumSize(btnSize);
        spamButton.addActionListener(e -> {
            if (!isSpammingNotifications) {
                isSpammingNotifications = true;
                spamButton.setText("ОТКЛЮЧИТЬ СПАМ УВЕДОМЛЕНИЙ");
                spamButton.setBackground(Color.RED);
                spamButton.setOpaque(true);
                
                notificationScheduler = Executors.newSingleThreadScheduledExecutor();
                notificationScheduler.scheduleAtFixedRate(() -> {
                    runAppleScript("display notification \"macOS hacked.\" with title \"Внимание\" sound name \"Basso\"");
                }, 0, 1, TimeUnit.SECONDS);
            } else {
                isSpammingNotifications = false;
                spamButton.setText("Включить спам-уведомления");
                spamButton.setBackground(null);
                if (notificationScheduler != null && !notificationScheduler.isShutdown()) {
                    notificationScheduler.shutdownNow();
                }
            }
        });
        panel.add(spamButton);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel ttsLabel = new JLabel("Текст для голосового спама (раз в 2 сек):");
        ttsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ttsLabel);

        JTextField ttsField = new JTextField("Система взломана");
        ttsField.setMaximumSize(new Dimension(340, 30));
        ttsField.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(ttsField);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        JButton ttsButton = new JButton("Включить голосовой спам");
        ttsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        ttsButton.setMaximumSize(btnSize);
        ttsButton.addActionListener(e -> {
            if (!isSpammingSpeech) {
                isSpammingSpeech = true;
                ttsButton.setText("ОТКЛЮЧИТЬ ГОЛОСОВОЙ СПАМ");
                ttsButton.setBackground(Color.ORANGE);
                ttsButton.setOpaque(true);
                ttsField.setEnabled(false);

                String textToSay = ttsField.getText().trim();
                
                speechScheduler = Executors.newSingleThreadScheduledExecutor();
                speechScheduler.scheduleAtFixedRate(() -> {
                    executeCommand(new String[]{"say", textToSay});
                }, 0, 2, TimeUnit.SECONDS);
            } else {
                isSpammingSpeech = false;
                ttsButton.setText("Включить голосовой спам");
                ttsButton.setBackground(null);
                ttsField.setEnabled(true);
                if (speechScheduler != null && !speechScheduler.isShutdown()) {
                    speechScheduler.shutdownNow();
                }
            }
        });
        panel.add(ttsButton);

        return panel;
    }

    private static JPanel createLogsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Живой поток системных логов macOS (Ошибки и Предупреждения):");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(titleLabel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("Очистить экран логов");
        clearButton.addActionListener(e -> logArea.setText(""));
        panel.add(clearButton, BorderLayout.SOUTH);

        return panel;
    }

    private static void startStreamingMacLogs() {
        if (isReadingLogs) return;
        isReadingLogs = true;

        new Thread(() -> {
            try {
                String[] command = {"log", "stream"};
                logProcess = new ProcessBuilder(command).start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
                String line;
                
                while (isReadingLogs && (line = reader.readLine()) != null) {
                    final String logLine = line;
                    SwingUtilities.invokeLater(() -> {
                        if (logArea != null) {
                            logArea.append(logLine + "\n");
                            logArea.setCaretPosition(logArea.getDocument().getLength());
                        }
                    });
                }
            } catch (IOException e) {
                System.err.println("Ошибка чтения логов: " + e.getMessage());
            }
        }).start();
    }

    private static JPanel createAboutTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("<html><center><h2>Hack my macOS 1</h2><p>Версия 1.0</p><br><p>Copyright (c) 2026 TimoNiki </p></center></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label);
        return panel;
    }

    private static void runAppleScript(String script) {
        executeCommand(new String[]{"osascript", "-e", script});
    }

    private static void executeCommand(String[] command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            pb.start();
        } catch (IOException e) {
            System.err.println("Ошибка выполнения: " + e.getMessage());
        }
    }
}
