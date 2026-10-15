package Edutrack;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {

    private static final long serialVersionUID = 1L;

    private final List<String> records = new ArrayList<>();

    private JTextArea output;
    private JTextField searchField;

    // Your actual dataset location:
    // D:\DSA-2\DSA\src\Edutrack\data
    private static final String DATA_FOLDER = "src/Edutrack/data";

    public Main() {

        setTitle("EduTrack - Intelligent University Academic Platform");

        setSize(1200, 750);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        createGUI();
    }

    // =========================================================
    // GUI
    // =========================================================

    private void createGUI() {

        setLayout(new BorderLayout(10, 10));

        // ---------------- TITLE ----------------

        JLabel title = new JLabel(
                "EduTrack - Intelligent University Academic Platform",
                SwingConstants.CENTER
        );

        title.setFont(new Font("Arial", Font.BOLD, 28));

        add(title, BorderLayout.NORTH);

        // ---------------- MENU ----------------

        JPanel menuPanel = new JPanel();

        menuPanel.setLayout(new GridLayout(11, 1, 10, 10));

        JButton loadButton = new JButton("Load Dataset");
        JButton showButton = new JButton("Show Dataset");
        JButton searchButton = new JButton("Search Text");
        JButton stringButton = new JButton("String Algorithms");
        JButton suffixButton = new JButton("Suffix Structures");
        JButton dpButton = new JButton("Advanced DP");
        JButton flowButton = new JButton("Network Flow");
        JButton npButton = new JButton("NP-Completeness");
        JButton randomButton = new JButton("Randomized & Parallel");
        JButton clearButton = new JButton("Clear Output");
        JButton exitButton = new JButton("Exit");

        menuPanel.add(loadButton);
        menuPanel.add(showButton);
        menuPanel.add(searchButton);
        menuPanel.add(stringButton);
        menuPanel.add(suffixButton);
        menuPanel.add(dpButton);
        menuPanel.add(flowButton);
        menuPanel.add(npButton);
        menuPanel.add(randomButton);
        menuPanel.add(clearButton);
        menuPanel.add(exitButton);

        add(menuPanel, BorderLayout.WEST);

        // ---------------- OUTPUT ----------------

        output = new JTextArea();

        output.setFont(
                new Font("Monospaced", Font.PLAIN, 14)
        );

        output.setEditable(false);

        JScrollPane scrollPane =
                new JScrollPane(output);

        add(scrollPane, BorderLayout.CENTER);

        // ---------------- SEARCH ----------------

        JPanel searchPanel =
                new JPanel(new BorderLayout(5, 5));

        JLabel searchLabel =
                new JLabel("Search Word / Pattern:");

        searchField =
                new JTextField();

        JButton searchNowButton =
                new JButton("Search");

        searchPanel.add(
                searchLabel,
                BorderLayout.WEST
        );

        searchPanel.add(
                searchField,
                BorderLayout.CENTER
        );

        searchPanel.add(
                searchNowButton,
                BorderLayout.EAST
        );

        searchPanel.setPreferredSize(
                new Dimension(100, 50)
        );

        add(searchPanel, BorderLayout.SOUTH);

        // =====================================================
        // BUTTON EVENTS
        // =====================================================

        loadButton.addActionListener(
                e -> loadDataset()
        );

        showButton.addActionListener(
                e -> showDataset()
        );

        searchButton.addActionListener(
                e -> searchDataset()
        );

        searchNowButton.addActionListener(
                e -> searchDataset()
        );

        stringButton.addActionListener(
                e -> runStringAlgorithms()
        );

        suffixButton.addActionListener(
                e -> runSuffixStructures()
        );

        dpButton.addActionListener(
                e -> runAdvancedDP()
        );

        flowButton.addActionListener(
                e -> runNetworkFlow()
        );

        npButton.addActionListener(
                e -> runNPCompleteness()
        );

        randomButton.addActionListener(
                e -> runRandomizedParallel()
        );

        clearButton.addActionListener(
                e -> output.setText("")
        );

        exitButton.addActionListener(
                e -> System.exit(0)
        );
    }

    // =========================================================
    // FIND DATA FOLDER
    // =========================================================

    private File findDataFolder() {

        // First try:
        // src/Edutrack/data
        File folder =
                new File(DATA_FOLDER);

        if (folder.exists() && folder.isDirectory()) {
            return folder;
        }

        // Try absolute project structure
        File folder2 =
                new File(
                        System.getProperty("user.dir"),
                        "src/Edutrack/data"
                );

        if (folder2.exists() && folder2.isDirectory()) {
            return folder2;
        }

        // Try if program is running from src
        File folder3 =
                new File(
                        System.getProperty("user.dir"),
                        "Edutrack/data"
                );

        if (folder3.exists() && folder3.isDirectory()) {
            return folder3;
        }

        return null;
    }

    // =========================================================
    // LOAD DATASET
    // =========================================================

    private void loadDataset() {

        records.clear();

        File folder =
                findDataFolder();

        if (folder == null) {

            output.setText(
                    "ERROR: Data folder not found.\n\n"
                    + "Program searched for:\n\n"
                    + "src/Edutrack/data\n\n"
                    + "Your project should contain:\n\n"
                    + "DSA-2\n"
                    + " └── DSA\n"
                    + "     └── src\n"
                    + "         └── Edutrack\n"
                    + "             ├── Main.java\n"
                    + "             ├── StringAlgorithms.java\n"
                    + "             ├── SuffixStructures.java\n"
                    + "             ├── AdvancedDP.java\n"
                    + "             └── data\n"
                    + "                 ├── assessments.csv\n"
                    + "                 ├── courses.csv\n"
                    + "                 ├── studentAssessment.csv\n"
                    + "                 ├── studentInfo.csv\n"
                    + "                 ├── studentRegistration.csv\n"
                    + "                 ├── studentVle.csv\n"
                    + "                 └── vle.csv\n\n"
                    + "Current working directory:\n"
                    + System.getProperty("user.dir")
            );

            return;
        }

        File[] files =
                folder.listFiles();

        if (files == null) {

            output.setText(
                    "Unable to read data folder."
            );

            return;
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                 EDUTRACK DATA LOADER\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Data folder:\n"
        );

        result.append(
                folder.getAbsolutePath()
        );

        result.append("\n\n");

        int totalRecords = 0;

        for (File file : files) {

            if (!file.isFile()) {
                continue;
            }

            String fileName =
                    file.getName().toLowerCase();

            if (!fileName.endsWith(".csv")) {
                continue;
            }

            int count =
                    loadCSVFile(file);

            totalRecords += count;

            result.append(
                    String.format(
                            "%-30s : %d records%n",
                            file.getName(),
                            count
                    )
            );
        }

        result.append(
                "\n------------------------------------------------------------\n"
        );

        result.append(
                "TOTAL RECORDS LOADED : "
        );

        result.append(totalRecords);

        result.append(
                "\n------------------------------------------------------------\n\n"
        );

        result.append(
                "Dataset successfully loaded.\n"
        );

        result.append(
                "\nYou can now use:\n"
        );

        result.append(
                "  Search Text\n"
        );

        result.append(
                "  String Algorithms\n"
        );

        result.append(
                "  Suffix Structures\n"
        );

        result.append(
                "  Advanced DP\n"
        );

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // LOAD CSV
    // =========================================================

    private int loadCSVFile(File file) {

        int count = 0;

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new FileReader(file)
                        )
        ) {

            String line;

            boolean firstLine = true;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                // Skip header
                if (firstLine) {

                    firstLine = false;

                    continue;
                }

                records.add(
                        file.getName()
                        + " | "
                        + line
                );

                count++;
            }

        } catch (IOException e) {

            output.append(
                    "\nError reading "
                    + file.getName()
                    + ": "
                    + e.getMessage()
            );
        }

        return count;
    }

    // =========================================================
    // SHOW DATASET
    // =========================================================

    private void showDataset() {

        if (records.isEmpty()) {

            loadDataset();

            if (records.isEmpty()) {
                return;
            }
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                    EDUTRACK DATASET\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Total records loaded : "
        );

        result.append(
                records.size()
        );

        result.append("\n\n");

        result.append(
                "Showing first 30 records:\n\n"
        );

        int limit =
                Math.min(
                        30,
                        records.size()
                );

        for (int i = 0; i < limit; i++) {

            result.append(
                    "Record #"
            );

            result.append(i + 1);

            result.append("\n");

            result.append(
                    "------------------------------------------------------------\n"
            );

            result.append(
                    records.get(i)
            );

            result.append("\n\n");
        }

        if (records.size() > 30) {

            result.append(
                    "... "
                    + (records.size() - 30)
                    + " more records loaded.\n"
            );
        }

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // NORMAL SEARCH
    // =========================================================

    private void searchDataset() {

        if (records.isEmpty()) {

            loadDataset();

            if (records.isEmpty()) {
                return;
            }
        }

        String pattern =
                searchField.getText().trim();

        if (pattern.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a word or pattern.",
                    "Search",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String lowerPattern =
                pattern.toLowerCase();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                       TEXT SEARCH\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Pattern : "
        );

        result.append(pattern);

        result.append("\n\n");

        int recordsFound = 0;
        int occurrences = 0;

        for (int i = 0;
             i < records.size();
             i++) {

            String record =
                    records.get(i);

            String lowerRecord =
                    record.toLowerCase();

            List<Integer> positions =
                    new ArrayList<>();

            int start = 0;

            while (true) {

                int position =
                        lowerRecord.indexOf(
                                lowerPattern,
                                start
                        );

                if (position == -1) {
                    break;
                }

                positions.add(position);

                occurrences++;

                start =
                        position + 1;
            }

            if (!positions.isEmpty()) {

                recordsFound++;

                result.append(
                        "------------------------------------------------------------\n"
                );

                result.append(
                        "Record #"
                );

                result.append(i + 1);

                result.append("\n");

                result.append(
                        "Source: "
                );

                result.append(
                        getSourceName(record)
                );

                result.append("\n");

                result.append(
                        "Positions: "
                );

                result.append(
                        positions
                );

                result.append("\n");

                result.append(
                        "Text: "
                );

                result.append(
                        getRecordText(record)
                );

                result.append("\n\n");
            }
        }

        result.append(
                "============================================================\n"
        );

        if (recordsFound == 0) {

            result.append(
                    "Pattern was NOT found.\n"
            );

        } else {

            result.append(
                    "Pattern found successfully.\n\n"
            );

            result.append(
                    "Records containing pattern : "
            );

            result.append(
                    recordsFound
            );

            result.append("\n");

            result.append(
                    "Total occurrences          : "
            );

            result.append(
                    occurrences
            );

            result.append("\n");
        }

        result.append(
                "============================================================\n"
        );

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // SOURCE NAME
    // =========================================================

    private String getSourceName(
            String record) {

        int separator =
                record.indexOf(" | ");

        if (separator == -1) {
            return "Unknown";
        }

        return record.substring(
                0,
                separator
        );
    }

    // =========================================================
    // RECORD TEXT
    // =========================================================

    private String getRecordText(
            String record) {

        int separator =
                record.indexOf(" | ");

        if (separator == -1) {
            return record;
        }

        return record.substring(
                separator + 3
        );
    }

    // =========================================================
    // CO1 - STRING ALGORITHMS
    // =========================================================

    private void runStringAlgorithms() {

        if (records.isEmpty()) {

            loadDataset();

            if (records.isEmpty()) {
                return;
            }
        }

        String pattern =
                searchField.getText().trim();

        if (pattern.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter a search word first.\n\n"
                    + "Example: student",
                    "String Algorithms",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                 CO1 - STRING ALGORITHMS\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Pattern: "
        );

        result.append(pattern);

        result.append("\n");

        result.append(
                "Records: "
        );

        result.append(
                records.size()
        );

        result.append("\n\n");

        // Use the first records for demonstration
        int limit =
                Math.min(
                        10,
                        records.size()
                );

        for (int i = 0;
             i < limit;
             i++) {

            String text =
                    getRecordText(
                            records.get(i)
                    );

            result.append(
                    "------------------------------------------------------------\n"
            );

            result.append(
                    "Record #"
            );

            result.append(i + 1);

            result.append("\n");

            result.append(
                    "Text: "
            );

            result.append(text);

            result.append("\n\n");

            result.append(
                    "NAIVE positions : "
            );

            result.append(
                    findNaive(text, pattern)
            );

            result.append("\n");

            result.append(
                    "KMP positions   : "
            );

            result.append(
                    findKMP(text, pattern)
            );

            result.append("\n");

            result.append(
                    "Z positions     : "
            );

            result.append(
                    findZ(text, pattern)
            );

            result.append("\n");

            result.append(
                    "Rabin-Karp      : "
            );

            result.append(
                    findRabinKarp(text, pattern)
            );

            result.append("\n\n");
        }

        result.append(
                "============================================================\n"
        );

        result.append(
                "Aho-Corasick can search multiple keywords.\n"
        );

        result.append(
                "Use comma-separated patterns in the search box.\n"
        );

        result.append(
                "Example: student, course, assessment\n"
        );

        result.append(
                "============================================================\n"
        );

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // NAIVE
    // =========================================================

    private List<Integer> findNaive(
            String text,
            String pattern) {

        List<Integer> result =
                new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.length() == 0 ||
                pattern.length() > text.length()) {

            return result;
        }

        for (int i = 0;
             i <= text.length() - pattern.length();
             i++) {

            int j = 0;

            while (
                    j < pattern.length()
                    &&
                    text.charAt(i + j)
                            == pattern.charAt(j)
            ) {

                j++;
            }

            if (j == pattern.length()) {
                result.add(i);
            }
        }

        return result;
    }

    // =========================================================
    // KMP
    // =========================================================

    private List<Integer> findKMP(
            String text,
            String pattern) {

        List<Integer> result =
                new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.isEmpty()) {
            return result;
        }

        int[] lps =
                new int[pattern.length()];

        int len = 0;

        for (int i = 1;
             i < pattern.length();) {

            if (
                    pattern.charAt(i)
                            == pattern.charAt(len)
            ) {

                lps[i] = ++len;
                i++;

            } else if (len > 0) {

                len =
                        lps[len - 1];

            } else {

                lps[i] = 0;
                i++;
            }
        }

        int i = 0;
        int j = 0;

        while (i < text.length()) {

            if (
                    text.charAt(i)
                            == pattern.charAt(j)
            ) {

                i++;
                j++;

                if (j == pattern.length()) {

                    result.add(i - j);

                    j =
                            lps[j - 1];
                }

            } else if (j > 0) {

                j =
                        lps[j - 1];

            } else {

                i++;
            }
        }

        return result;
    }

    // =========================================================
    // Z ALGORITHM
    // =========================================================

    private List<Integer> findZ(
            String text,
            String pattern) {

        List<Integer> result =
                new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.isEmpty()) {
            return result;
        }

        String combined =
                pattern + "$" + text;

        int n =
                combined.length();

        int[] z =
                new int[n];

        int left = 0;
        int right = 0;

        for (int i = 1; i < n; i++) {

            if (i <= right) {

                z[i] =
                        Math.min(
                                right - i + 1,
                                z[i - left]
                        );
            }

            while (
                    i + z[i] < n
                    &&
                    combined.charAt(z[i])
                            ==
                            combined.charAt(i + z[i])
            ) {

                z[i]++;
            }

            if (
                    i + z[i] - 1
                            > right
            ) {

                left = i;

                right =
                        i + z[i] - 1;
            }
        }

        for (int i = 0; i < n; i++) {

            if (
                    z[i]
                            == pattern.length()
            ) {

                result.add(
                        i - pattern.length() - 1
                );
            }
        }

        return result;
    }

    // =========================================================
    // RABIN-KARP
    // =========================================================

    private List<Integer> findRabinKarp(
            String text,
            String pattern) {

        List<Integer> result =
                new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();

        if (
                m == 0 ||
                m > n
        ) {
            return result;
        }

        final long BASE = 256;
        final long MOD = 1000000007;

        long patternHash = 0;
        long textHash = 0;
        long highestPower = 1;

        for (int i = 0; i < m - 1; i++) {

            highestPower =
                    (highestPower * BASE)
                            % MOD;
        }

        for (int i = 0; i < m; i++) {

            patternHash =
                    (
                            patternHash * BASE
                            + pattern.charAt(i)
                    ) % MOD;

            textHash =
                    (
                            textHash * BASE
                            + text.charAt(i)
                    ) % MOD;
        }

        for (int i = 0;
             i <= n - m;
             i++) {

            if (
                    patternHash
                            == textHash
            ) {

                boolean match = true;

                for (int j = 0;
                     j < m;
                     j++) {

                    if (
                            text.charAt(i + j)
                                    != pattern.charAt(j)
                    ) {

                        match = false;
                        break;
                    }
                }

                if (match) {
                    result.add(i);
                }
            }

            if (i < n - m) {

                textHash =
                        (
                                BASE *
                                (
                                        textHash
                                        -
                                        text.charAt(i)
                                        * highestPower
                                )
                                +
                                text.charAt(i + m)
                        ) % MOD;

                if (textHash < 0) {
                    textHash += MOD;
                }
            }
        }

        return result;
    }

    // =========================================================
    // CO2 - SUFFIX STRUCTURES
    // =========================================================

    private void runSuffixStructures() {

        if (records.isEmpty()) {

            loadDataset();

            if (records.isEmpty()) {
                return;
            }
        }

        String text =
                getRecordText(
                        records.get(0)
                );

        String pattern =
                searchField.getText().trim();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                 CO2 - SUFFIX STRUCTURES\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Using first EduTrack dataset record.\n\n"
        );

        result.append(
                "Text:\n"
        );

        result.append(text);

        result.append("\n\n");

        result.append(
                SuffixStructures.displaySuffixArray(
                        text
                )
        );

        result.append("\n");

        result.append(
                SuffixStructures.displayLCP(
                        text
                )
        );

        if (!pattern.isEmpty()) {

            result.append("\n");

            result.append(
                    SuffixStructures.displaySearch(
                            text,
                            pattern
                    )
            );
        }

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // CO3 - ADVANCED DP
    // =========================================================

    private void runAdvancedDP() {

        String word =
                searchField.getText().trim();

        if (word.isEmpty()) {
            word = "student";
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "             CO3 - ADVANCED DYNAMIC PROGRAMMING\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Input word: "
        );

        result.append(word);

        result.append("\n\n");

        // -----------------------------------------------------
        // Levenshtein
        // -----------------------------------------------------

        result.append(
                "1. LEVENSHTEIN DISTANCE\n"
        );

        result.append(
                "student -> "
        );

        result.append(word);

        result.append(" = ");

        result.append(
                AdvancedDP.levenshtein(
                        "student",
                        word
                )
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Damerau
        // -----------------------------------------------------

        result.append(
                "2. DAMERAU-LEVENSHTEIN\n"
        );

        result.append(
                "student -> "
        );

        result.append(word);

        result.append(" = ");

        result.append(
                AdvancedDP.damerauLevenshtein(
                        "student",
                        word
                )
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Needleman Wunsch
        // -----------------------------------------------------

        result.append(
                "3. NEEDLEMAN-WUNSCH\n"
        );

        result.append(
                "student vs "
        );

        result.append(word);

        result.append(" = ");

        result.append(
                AdvancedDP.needlemanWunsch(
                        "student",
                        word
                )
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Smith Waterman
        // -----------------------------------------------------

        result.append(
                "4. SMITH-WATERMAN\n"
        );

        result.append(
                "student vs "
        );

        result.append(word);

        result.append(" = ");

        result.append(
                AdvancedDP.smithWaterman(
                        "student",
                        word
                )
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Bitmask
        // -----------------------------------------------------

        int[][] cost = {

                {9, 2, 7},

                {6, 4, 3},

                {5, 8, 1}

        };

        result.append(
                "5. BITMASK DP\n"
        );

        result.append(
                "Minimum assignment cost = "
        );

        result.append(
                AdvancedDP.bitmaskDP(cost)
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Matrix Chain
        // -----------------------------------------------------

        int[] dimensions = {
                10, 30, 5, 60
        };

        result.append(
                "6. MATRIX CHAIN MULTIPLICATION\n"
        );

        result.append(
                "Minimum multiplication cost = "
        );

        result.append(
                AdvancedDP.matrixChain(
                        dimensions
                )
        );

        result.append("\n\n");

        // -----------------------------------------------------
        // Optimal BST
        // -----------------------------------------------------

        int[] frequency = {
                34, 8, 50
        };

        result.append(
                "7. OPTIMAL BINARY SEARCH TREE\n"
        );

        result.append(
                "Minimum search cost = "
        );

        result.append(
                AdvancedDP.optimalBST(
                        frequency
                )
        );

        result.append("\n\n");

        result.append(
                "============================================================\n"
        );

        result.append(
                "Advanced DP algorithms executed successfully.\n"
        );

        result.append(
                "============================================================\n"
        );

        output.setText(
                result.toString()
        );
    }

    // =========================================================
    // CO4 - NETWORK FLOW
    // =========================================================

    private void runNetworkFlow() {

        File folder =
                findDataFolder();

        if (folder == null) {

            output.setText(
                    "ERROR: Data folder not found.\n"
                    + "Network Flow needs courses.csv and studentInfo.csv."
            );

            return;
        }

        output.setText(
                "Running Network Flow algorithms...\n"
        );

        // run off the GUI thread: studentInfo.csv is a large file
        final File dataFolder = folder;

        new Thread(
                () -> {

                    String report =
                            NetworkFlow.runAll(dataFolder);

                    SwingUtilities.invokeLater(
                            () -> {
                                output.setText(report);
                                output.setCaretPosition(0);
                            }
                    );
                }
        ).start();
    }

    // =========================================================
    // CO5 - NP-COMPLETENESS AND APPROXIMATION
    // =========================================================

    private void runNPCompleteness() {

        File folder =
                findDataFolder();

        if (folder == null) {

            output.setText(
                    "ERROR: Data folder not found.\n"
                    + "NP-Completeness needs studentInfo.csv."
            );

            return;
        }

        output.setText(
                "Running NP-Completeness algorithms...\n"
        );

        final File dataFolder = folder;

        new Thread(
                () -> {

                    String report =
                            NPCompleteness.runAll(dataFolder);

                    SwingUtilities.invokeLater(
                            () -> {
                                output.setText(report);
                                output.setCaretPosition(0);
                            }
                    );
                }
        ).start();
    }

    // =========================================================
    // CO6 - RANDOMIZED AND PARALLEL ALGORITHMS
    // =========================================================

    private void runRandomizedParallel() {

        File folder =
                findDataFolder();

        if (folder == null) {

            output.setText(
                    "ERROR: Data folder not found.\n"
                    + "Randomized & Parallel needs studentAssessment.csv and studentVle.csv."
            );

            return;
        }

        output.setText(
                "Running Randomized and Parallel algorithms...\n"
                + "(streaming up to 5,000,000 activity rows, please wait)\n"
        );

        final File dataFolder = folder;

        new Thread(
                () -> {

                    String report =
                            RandomizedParallel.runAll(dataFolder);

                    SwingUtilities.invokeLater(
                            () -> {
                                output.setText(report);
                                output.setCaretPosition(0);
                            }
                    );
                }
        ).start();
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    Main application =
                            new Main();

                    application.setVisible(true);
                }
        );
    }
}
