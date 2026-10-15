package Edutrack;

import java.util.Arrays;

public class AdvancedDP {

    // =========================================================
    // 1. LEVENSHTEIN / WAGNER-FISCHER
    // =========================================================
    public static int levenshtein(String a, String b) {

        a = a.toLowerCase();
        b = b.toLowerCase();

        int m = a.length();
        int n = b.length();

        // dp[i][j] = minimum operations to convert
        // first i characters of a into first j characters of b
        int[][] dp = new int[m + 1][n + 1];

        // Convert a string into empty string
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        // Convert empty string into b
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {

            for (int j = 1; j <= n; j++) {

                if (a.charAt(i - 1) == b.charAt(j - 1)) {

                    // Characters are already equal
                    dp[i][j] = dp[i - 1][j - 1];

                } else {

                    // Choose minimum of:
                    // insertion, deletion, substitution
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j],          // deletion
                            Math.min(
                                    dp[i][j - 1], // insertion
                                    dp[i - 1][j - 1] // substitution
                            )
                    );
                }
            }
        }

        return dp[m][n];
    }


    // =========================================================
    // 2. DAMERAU-LEVENSHTEIN
    // =========================================================
    public static int damerauLevenshtein(String a, String b) {

        a = a.toLowerCase();
        b = b.toLowerCase();

        int m = a.length();
        int n = b.length();

        int[][] dp = new int[m + 1][n + 1];

        // Base cases
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {

            for (int j = 1; j <= n; j++) {

                int cost;

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Normal Levenshtein operations
                dp[i][j] = Math.min(
                        dp[i - 1][j] + 1,          // deletion
                        Math.min(
                                dp[i][j - 1] + 1,  // insertion
                                dp[i - 1][j - 1] + cost // substitution
                        )
                );

                // Adjacent character transposition
                if (i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1)) {

                    dp[i][j] = Math.min(
                            dp[i][j],
                            dp[i - 2][j - 2] + 1
                    );
                }
            }
        }

        return dp[m][n];
    }


    // =========================================================
    // 3. NEEDLEMAN-WUNSCH
    // =========================================================
    public static int needlemanWunsch(String a, String b) {

        a = a.toLowerCase();
        b = b.toLowerCase();

        int m = a.length();
        int n = b.length();

        // Scoring system
        int MATCH = 1;
        int MISMATCH = -1;
        int GAP = -2;

        int[][] dp = new int[m + 1][n + 1];

        // First row
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j * GAP;
        }

        // First column
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i * GAP;
        }

        for (int i = 1; i <= m; i++) {

            for (int j = 1; j <= n; j++) {

                int diagonal;

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    diagonal = dp[i - 1][j - 1] + MATCH;
                } else {
                    diagonal = dp[i - 1][j - 1] + MISMATCH;
                }

                int up = dp[i - 1][j] + GAP;

                int left = dp[i][j - 1] + GAP;

                dp[i][j] = Math.max(
                        diagonal,
                        Math.max(up, left)
                );
            }
        }

        return dp[m][n];
    }


    // =========================================================
    // 4. SMITH-WATERMAN
    // =========================================================
    public static int smithWaterman(String a, String b) {

        a = a.toLowerCase();
        b = b.toLowerCase();

        int m = a.length();
        int n = b.length();

        int MATCH = 2;
        int MISMATCH = -1;
        int GAP = -2;

        int[][] dp = new int[m + 1][n + 1];

        int bestScore = 0;

        for (int i = 1; i <= m; i++) {

            for (int j = 1; j <= n; j++) {

                int diagonal;

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    diagonal = dp[i - 1][j - 1] + MATCH;
                } else {
                    diagonal = dp[i - 1][j - 1] + MISMATCH;
                }

                int up = dp[i - 1][j] + GAP;
                int left = dp[i][j - 1] + GAP;

                // 0 is important:
                // it allows a new local alignment to start
                dp[i][j] = Math.max(
                        0,
                        Math.max(
                                diagonal,
                                Math.max(up, left)
                        )
                );

                bestScore = Math.max(
                        bestScore,
                        dp[i][j]
                );
            }
        }

        return bestScore;
    }


    // =========================================================
    // 5. BITMASK DP
    // =========================================================
    public static int bitmaskDP(int[][] cost) {

        int n = cost.length;

        int totalMasks = 1 << n;

        int INF = Integer.MAX_VALUE / 4;

        // dp[mask] = minimum cost after assigning
        // tasks represented by mask
        int[] dp = new int[totalMasks];

        Arrays.fill(dp, INF);

        dp[0] = 0;

        for (int mask = 0; mask < totalMasks; mask++) {

            // Number of assigned tasks
            int task = Integer.bitCount(mask);

            if (task >= n) {
                continue;
            }

            for (int resource = 0; resource < n; resource++) {

                // Check whether resource is already used
                if ((mask & (1 << resource)) == 0) {

                    int newMask =
                            mask | (1 << resource);

                    dp[newMask] = Math.min(
                            dp[newMask],
                            dp[mask] + cost[task][resource]
                    );
                }
            }
        }

        return dp[totalMasks - 1];
    }


    // =========================================================
    // 6. MATRIX CHAIN MULTIPLICATION
    // =========================================================
    public static long matrixChain(int[] dimensions) {

        /*
         * If dimensions = {10,30,5,60,10}
         *
         * Matrices are:
         *
         * A1 = 10 x 30
         * A2 = 30 x 5
         * A3 = 5 x 60
         * A4 = 60 x 10
         */

        int n = dimensions.length - 1;

        if (n <= 0) {
            return 0;
        }

        long[][] dp = new long[n][n];

        // length = number of matrices in chain
        for (int length = 2; length <= n; length++) {

            for (int i = 0; i <= n - length; i++) {

                int j = i + length - 1;

                dp[i][j] = Long.MAX_VALUE;

                // Try every possible split
                for (int k = i; k < j; k++) {

                    long cost =
                            dp[i][k]
                            + dp[k + 1][j]
                            + (long) dimensions[i]
                            * dimensions[k + 1]
                            * dimensions[j + 1];

                    dp[i][j] =
                            Math.min(dp[i][j], cost);
                }
            }
        }

        return dp[0][n - 1];
    }


    // =========================================================
    // 7. OPTIMAL BINARY SEARCH TREE
    // =========================================================
    public static int optimalBST(int[] frequency) {

        int n = frequency.length;

        if (n == 0) {
            return 0;
        }

        int[][] dp = new int[n][n];

        // Prefix sums for frequencies
        int[] prefix = new int[n + 1];

        for (int i = 0; i < n; i++) {
            prefix[i + 1] =
                    prefix[i] + frequency[i];
        }

        // One key
        for (int i = 0; i < n; i++) {
            dp[i][i] = frequency[i];
        }

        // Chain lengths
        for (int length = 2; length <= n; length++) {

            for (int i = 0; i <= n - length; i++) {

                int j = i + length - 1;

                dp[i][j] = Integer.MAX_VALUE;

                int totalFrequency =
                        prefix[j + 1] - prefix[i];

                // Try every key as root
                for (int root = i; root <= j; root++) {

                    int left =
                            (root > i)
                            ? dp[i][root - 1]
                            : 0;

                    int right =
                            (root < j)
                            ? dp[root + 1][j]
                            : 0;

                    int cost =
                            left + right + totalFrequency;

                    dp[i][j] =
                            Math.min(dp[i][j], cost);
                }
            }
        }

        return dp[0][n - 1];
    }
}