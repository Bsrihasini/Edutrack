package Edutrack;

import java.util.Arrays;
import java.util.Comparator;

public class SuffixStructures {

    // =========================================================
    // 1. BUILD SUFFIX ARRAY
    // =========================================================
    // A suffix is the part of the string starting from a position.
    // Example: "CROP"
    // suffixes: CROP, ROP, OP, P
    //
    // We sort all suffixes alphabetically and store their
    // starting positions.
    // =========================================================

    public static int[] buildSuffixArray(String text) {

        if (text == null || text.length() == 0) {
            return new int[0];
        }

        text = text.toLowerCase();

        int n = text.length();

        Integer[] suffixes = new Integer[n];

        // Store every suffix starting position
        for (int i = 0; i < n; i++) {
            suffixes[i] = i;
        }

        // Sort suffixes lexicographically
        final String finalText = text;

        Arrays.sort(
                suffixes,
                new Comparator<Integer>() {

                    @Override
                    public int compare(Integer a, Integer b) {

                        return finalText
                                .substring(a)
                                .compareTo(
                                        finalText.substring(b)
                                );
                    }
                }
        );

        int[] suffixArray = new int[n];

        for (int i = 0; i < n; i++) {
            suffixArray[i] = suffixes[i];
        }

        return suffixArray;
    }


    // =========================================================
    // 2. SEARCH PATTERN USING SUFFIX ARRAY
    // =========================================================
    //
    // Since suffixes are sorted, we use binary search.
    //
    // If the pattern occurs in a suffix, that suffix position
    // is one possible occurrence of the pattern.
    // =========================================================

    public static int[] searchUsingSuffixArray(
            String text,
            String pattern) {

        if (text == null ||
            pattern == null ||
            text.length() == 0 ||
            pattern.length() == 0) {

            return new int[0];
        }

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int[] suffixArray =
                buildSuffixArray(text);

        int n = suffixArray.length;

        // -----------------------------------------------------
        // Find first possible matching suffix
        // -----------------------------------------------------

        int left = 0;
        int right = n - 1;

        int first = -1;

        while (left <= right) {

            int mid = (left + right) / 2;

            String suffix =
                    text.substring(
                            suffixArray[mid]
                    );

            if (suffix.startsWith(pattern)) {

                first = mid;
                right = mid - 1;

            } else if (
                    suffix.compareTo(pattern) < 0) {

                left = mid + 1;

            } else {

                right = mid - 1;
            }
        }

        if (first == -1) {
            return new int[0];
        }

        // -----------------------------------------------------
        // Find last matching suffix
        // -----------------------------------------------------

        left = first;
        right = n - 1;

        int last = first;

        while (left <= right) {

            int mid = (left + right) / 2;

            String suffix =
                    text.substring(
                            suffixArray[mid]
                    );

            if (suffix.startsWith(pattern)) {

                last = mid;
                left = mid + 1;

            } else {

                right = mid - 1;
            }
        }

        // -----------------------------------------------------
        // Store all matching positions
        // -----------------------------------------------------

        int count =
                last - first + 1;

        int[] positions =
                new int[count];

        for (int i = 0; i < count; i++) {

            positions[i] =
                    suffixArray[first + i];
        }

        // Sort positions according to their occurrence
        // in the original text.
        Arrays.sort(positions);

        return positions;
    }


    // =========================================================
    // 3. KASAI LCP ALGORITHM
    // =========================================================
    //
    // LCP = Longest Common Prefix.
    //
    // It tells how many starting characters are common between
    // two adjacent suffixes in the suffix array.
    //
    // Example:
    //
    // "academic"
    // "academy"
    //
    // Common prefix = "academ"
    // LCP = 6
    // =========================================================

    public static int[] buildLCP(
            String text,
            int[] suffixArray) {

        if (text == null ||
            text.length() == 0 ||
            suffixArray == null ||
            suffixArray.length == 0) {

            return new int[0];
        }

        text = text.toLowerCase();

        int n = text.length();

        int[] rank =
                new int[n];

        // rank[position] = position of suffix
        // in suffix array
        for (int i = 0; i < n; i++) {

            rank[suffixArray[i]] = i;
        }

        int[] lcp =
                new int[n];

        int k = 0;

        // Kasai algorithm
        for (int i = 0; i < n; i++) {

            int r = rank[i];

            // Last suffix has no next suffix
            if (r == n - 1) {

                k = 0;
                continue;
            }

            int j =
                    suffixArray[r + 1];

            // Compare characters
            // until they become different
            while (
                    i + k < n &&
                    j + k < n &&
                    text.charAt(i + k)
                    ==
                    text.charAt(j + k)
            ) {

                k++;
            }

            lcp[r] = k;

            // Reuse previous comparison
            if (k > 0) {
                k--;
            }
        }

        return lcp;
    }


    // =========================================================
    // DISPLAY SUFFIX ARRAY
    // =========================================================

    public static String displaySuffixArray(
            String text) {

        if (text == null ||
            text.length() == 0) {

            return "No text available.";
        }

        int[] suffixArray =
                buildSuffixArray(text);

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                    SUFFIX ARRAY\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Text       : "
                + text
                + "\n"
        );

        result.append(
                "Text Length: "
                + text.length()
                + "\n\n"
        );

        result.append(
                "Index\tPosition\tSuffix\n"
        );

        result.append(
                "------------------------------------------------------------\n"
        );

        for (int i = 0;
             i < suffixArray.length;
             i++) {

            int position =
                    suffixArray[i];

            result.append(
                    String.format(
                            "%d\t%d\t%s\n",
                            i,
                            position,
                            text.substring(position)
                    )
            );
        }

        result.append(
                "\n============================================================\n"
        );

        return result.toString();
    }


    // =========================================================
    // DISPLAY LCP ARRAY
    // =========================================================

    public static String displayLCP(
            String text) {

        if (text == null ||
            text.length() == 0) {

            return "No text available.";
        }

        int[] suffixArray =
                buildSuffixArray(text);

        int[] lcp =
                buildLCP(
                        text,
                        suffixArray
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                       LCP ARRAY\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Index\tSuffix Position\tLCP\n"
        );

        result.append(
                "------------------------------------------------------------\n"
        );

        for (int i = 0;
             i < lcp.length;
             i++) {

            result.append(
                    String.format(
                            "%d\t%d\t\t%d\n",
                            i,
                            suffixArray[i],
                            lcp[i]
                    )
            );
        }

        result.append(
                "\n============================================================\n"
        );

        return result.toString();
    }


    // =========================================================
    // SEARCH RESULT DISPLAY
    // =========================================================

    public static String displaySearch(
            String text,
            String pattern) {

        int[] positions =
                searchUsingSuffixArray(
                        text,
                        pattern
                );

        StringBuilder result =
                new StringBuilder();

        result.append(
                "============================================================\n"
        );

        result.append(
                "                 SUFFIX ARRAY SEARCH\n"
        );

        result.append(
                "============================================================\n\n"
        );

        result.append(
                "Text    : "
                + text
                + "\n"
        );

        result.append(
                "Pattern : "
                + pattern
                + "\n\n"
        );

        if (positions.length == 0) {

            result.append(
                    "Pattern not found.\n"
            );

        } else {

            result.append(
                    "Pattern found "
                    + positions.length
                    + " time(s).\n\n"
            );

            result.append(
                    "Positions in original text:\n"
            );

            for (int position : positions) {

                result.append(
                        "  Position "
                        + position
                        + " -> "
                        + text.substring(
                                position,
                                Math.min(
                                        position + pattern.length(),
                                        text.length()
                                )
                        )
                        + "\n"
                );
            }
        }

        result.append(
                "\n============================================================\n"
        );

        return result.toString();
    }
}