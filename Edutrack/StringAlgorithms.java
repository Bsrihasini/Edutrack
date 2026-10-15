package Edutrack;

import java.util.*;

public class StringAlgorithms {

    private List<String> records;

    public StringAlgorithms(List<String> records) {
        this.records = records;
    }

    // ============================================================
    // NAIVE STRING SEARCH
    // ============================================================

    private List<Integer> naiveSearch(String text, String pattern) {

        List<Integer> positions = new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return positions;
        }

        for (int i = 0; i <= n - m; i++) {

            int j = 0;

            while (j < m &&
                    text.charAt(i + j) == pattern.charAt(j)) {
                j++;
            }

            if (j == m) {
                positions.add(i);
            }
        }

        return positions;
    }

    // ============================================================
    // KMP
    // ============================================================

    private int[] buildLPS(String pattern) {

        int[] lps = new int[pattern.length()];

        int length = 0;
        int i = 1;

        while (i < pattern.length()) {

            if (pattern.charAt(i) == pattern.charAt(length)) {

                length++;
                lps[i] = length;
                i++;

            } else {

                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    private List<Integer> kmpSearch(String text, String pattern) {

        List<Integer> positions = new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        if (pattern.length() == 0) {
            return positions;
        }

        int[] lps = buildLPS(pattern);

        int i = 0;
        int j = 0;

        while (i < text.length()) {

            if (text.charAt(i) == pattern.charAt(j)) {

                i++;
                j++;

                if (j == pattern.length()) {

                    positions.add(i - j);

                    j = lps[j - 1];
                }

            } else {

                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return positions;
    }

    // ============================================================
    // Z FUNCTION
    // ============================================================

    private List<Integer> zSearch(String text, String pattern) {

        List<Integer> positions = new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        String combined = pattern + "$" + text;

        int n = combined.length();

        int[] z = new int[n];

        int left = 0;
        int right = 0;

        for (int i = 1; i < n; i++) {

            if (i <= right) {
                z[i] = Math.min(right - i + 1,
                        z[i - left]);
            }

            while (i + z[i] < n &&
                    combined.charAt(z[i]) ==
                    combined.charAt(i + z[i])) {

                z[i]++;
            }

            if (i + z[i] - 1 > right) {

                left = i;
                right = i + z[i] - 1;
            }
        }

        for (int i = 0; i < n; i++) {

            if (z[i] == pattern.length()) {

                int position =
                        i - pattern.length() - 1;

                if (position >= 0) {
                    positions.add(position);
                }
            }
        }

        return positions;
    }

    // ============================================================
    // RABIN-KARP
    // ============================================================

    private List<Integer> rabinKarpSearch(
            String text,
            String pattern) {

        List<Integer> positions = new ArrayList<>();

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();

        if (m == 0 || m > n) {
            return positions;
        }

        final long BASE = 256;
        final long MOD = 1000000007;

        long patternHash = 0;
        long textHash = 0;
        long highestPower = 1;

        for (int i = 0; i < m - 1; i++) {
            highestPower =
                    (highestPower * BASE) % MOD;
        }

        for (int i = 0; i < m; i++) {

            patternHash =
                    (patternHash * BASE
                            + pattern.charAt(i)) % MOD;

            textHash =
                    (textHash * BASE
                            + text.charAt(i)) % MOD;
        }

        for (int i = 0; i <= n - m; i++) {

            if (patternHash == textHash) {

                boolean match = true;

                for (int j = 0; j < m; j++) {

                    if (text.charAt(i + j)
                            != pattern.charAt(j)) {

                        match = false;
                        break;
                    }
                }

                if (match) {
                    positions.add(i);
                }
            }

            if (i < n - m) {

                textHash =
                        (BASE * (textHash
                                - text.charAt(i)
                                * highestPower)
                                + text.charAt(i + m))
                                % MOD;

                if (textHash < 0) {
                    textHash += MOD;
                }
            }
        }

        return positions;
    }

    // ============================================================
    // AHO-CORASICK
    // ============================================================

    private static class ACNode {

        Map<Character, Integer> next =
                new HashMap<>();

        int fail = 0;

        List<String> outputs =
                new ArrayList<>();
    }

    private List<String> searchAhoCorasick(
            String text,
            List<String> patterns) {

        List<String> results =
                new ArrayList<>();

        List<ACNode> trie =
                new ArrayList<>();

        trie.add(new ACNode());

        // Build trie
        for (String pattern : patterns) {

            pattern = pattern.toLowerCase();

            int current = 0;

            for (char c : pattern.toCharArray()) {

                if (!trie.get(current).next.containsKey(c)) {

                    trie.get(current).next.put(
                            c,
                            trie.size()
                    );

                    trie.add(new ACNode());
                }

                current =
                        trie.get(current).next.get(c);
            }

            trie.get(current).outputs.add(pattern);
        }

        // Build failure links
        Queue<Integer> queue =
                new LinkedList<>();

        for (int child :
                trie.get(0).next.values()) {

            trie.get(child).fail = 0;
            queue.add(child);
        }

        while (!queue.isEmpty()) {

            int current = queue.poll();

            for (Map.Entry<Character, Integer> entry :
                    trie.get(current).next.entrySet()) {

                char c = entry.getKey();

                int child = entry.getValue();

                queue.add(child);

                int failure =
                        trie.get(current).fail;

                while (failure != 0 &&
                        !trie.get(failure).next.containsKey(c)) {

                    failure =
                            trie.get(failure).fail;
                }

                if (trie.get(failure).next.containsKey(c)
                        && trie.get(failure).next.get(c) != child) {

                    failure =
                            trie.get(failure).next.get(c);
                }

                trie.get(child).fail = failure;

                trie.get(child).outputs.addAll(
                        trie.get(failure).outputs
                );
            }
        }

        text = text.toLowerCase();

        int current = 0;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            while (current != 0 &&
                    !trie.get(current).next.containsKey(c)) {

                current =
                        trie.get(current).fail;
            }

            if (trie.get(current).next.containsKey(c)) {

                current =
                        trie.get(current).next.get(c);
            }

            for (String match :
                    trie.get(current).outputs) {

                int position =
                        i - match.length() + 1;

                results.add(
                        match + "@" + position
                );
            }
        }

        return results;
    }

    // ============================================================
    // DISPLAY SEARCH RESULTS
    // ============================================================

    private void displayResults(
            String algorithm,
            String pattern) {

        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                         " + algorithm);
        System.out.println("================================================================================");

        System.out.println("Search Pattern : " + pattern);
        System.out.println("Records Scanned: " + records.size());

        int totalOccurrences = 0;

        for (int i = 0; i < records.size(); i++) {

            String record = records.get(i);

            List<Integer> positions;

            switch (algorithm) {

                case "NAIVE STRING SEARCH":
                    positions =
                            naiveSearch(record, pattern);
                    break;

                case "KMP STRING SEARCH":
                    positions =
                            kmpSearch(record, pattern);
                    break;

                case "Z-FUNCTION SEARCH":
                    positions =
                            zSearch(record, pattern);
                    break;

                case "RABIN-KARP SEARCH":
                    positions =
                            rabinKarpSearch(record, pattern);
                    break;

                default:
                    positions = new ArrayList<>();
            }

            if (!positions.isEmpty()) {

                System.out.println();
                System.out.println("Record #" + (i + 1));
                System.out.println("Text: " + record);

                for (int position : positions) {

                    System.out.println(
                            "  Found at character position: "
                                    + position
                    );

                    totalOccurrences++;
                }
            }
        }

        System.out.println();
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Total occurrences found: "
                + totalOccurrences);
        System.out.println("================================================================================");
    }

    // ============================================================
    // MENU
    // ============================================================

    public void runMenu(Scanner scanner) {

        while (true) {

            System.out.println();
            System.out.println("================================================================================");
            System.out.println("                         CO1 - STRING ALGORITHMS");
            System.out.println("================================================================================");

            System.out.println("1. Naive Search");
            System.out.println("2. KMP Search");
            System.out.println("3. Z-Function Search");
            System.out.println("4. Rabin-Karp Search");
            System.out.println("5. Aho-Corasick");
            System.out.println("6. Back");

            System.out.print("\nChoice: ");

            int choice =
                    Integer.parseInt(scanner.nextLine());

            if (choice == 6) {
                return;
            }

            if (choice >= 1 && choice <= 4) {

                System.out.print(
                        "Enter search word/pattern: "
                );

                String pattern =
                        scanner.nextLine();

                switch (choice) {

                    case 1:
                        displayResults(
                                "NAIVE STRING SEARCH",
                                pattern
                        );
                        break;

                    case 2:
                        displayResults(
                                "KMP STRING SEARCH",
                                pattern
                        );
                        break;

                    case 3:
                        displayResults(
                                "Z-FUNCTION SEARCH",
                                pattern
                        );
                        break;

                    case 4:
                        displayResults(
                                "RABIN-KARP SEARCH",
                                pattern
                        );
                        break;
                }

            } else if (choice == 5) {

                System.out.println();
                System.out.println(
                        "Enter multiple keywords separated by comma."
                );

                System.out.print("Keywords: ");

                String input =
                        scanner.nextLine();

                String[] parts =
                        input.split(",");

                List<String> patterns =
                        new ArrayList<>();

                for (String p : parts) {

                    if (!p.trim().isEmpty()) {
                        patterns.add(p.trim());
                    }
                }

                System.out.println();
                System.out.println("================================================================================");
                System.out.println("                         AHO-CORASICK SEARCH");
                System.out.println("================================================================================");

                int total = 0;

                for (int i = 0; i < records.size(); i++) {

                    List<String> matches =
                            searchAhoCorasick(
                                    records.get(i),
                                    patterns
                            );

                    if (!matches.isEmpty()) {

                        System.out.println();
                        System.out.println(
                                "Record #" + (i + 1)
                        );

                        System.out.println(
                                "Text: " + records.get(i)
                        );

                        for (String match : matches) {

                            String[] data =
                                    match.split("@");

                            System.out.println(
                                    "  Keyword: "
                                            + data[0]
                                            + " | Position: "
                                            + data[1]
                            );

                            total++;
                        }
                    }
                }

                System.out.println();
                System.out.println(
                        "Total keyword occurrences: "
                                + total
                );

            } else {

                System.out.println(
                        "Invalid choice."
                );
            }
        }
    }
}