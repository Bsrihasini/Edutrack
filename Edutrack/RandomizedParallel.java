package Edutrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

/**
 * CO6 - RANDOMIZED AND PARALLEL ALGORITHMS
 *
 * 1. Randomized QuickSort -> rank academic records
 * 2. Reservoir Sampling   -> continuously arriving student-activity data
 * 3. Miller-Rabin         -> large numerical computations
 * 4. Blelloch Scan        -> cumulative academic statistics
 * 5. Parallel Reduce      -> student-performance aggregation
 * 6. Brent's theorem      -> analysis of parallel academic-data processing
 */
public class RandomizedParallel {

    // limit on streamed activity rows so the GUI stays responsive
    private static final long MAX_VLE_ROWS = 5_000_000L;

    private static final int WEEKS = 64; // power of two for Blelloch scan

    // =========================================================
    // DATA LOADING
    // =========================================================

    public static class Scores {

        public int[] allScores = new int[0];   // every individual score
        public int[] studentIds = new int[0];  // one entry per student
        public double[] average = new double[0];
        public boolean synthetic = false;
    }

    private static String clean(String s) {
        return s.replace("\"", "").trim();
    }

    // studentAssessment.csv: id_assessment,id_student,date_submitted,is_banked,score
    public static Scores loadScores(File folder) {

        Scores result = new Scores();

        File file = new File(folder, "studentAssessment.csv");

        Map<Integer, long[]> perStudent = new HashMap<>();
        int[] scores = new int[200_000];
        int count = 0;

        if (file.exists()) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {

                br.readLine(); // header

                String line;

                while ((line = br.readLine()) != null) {

                    String[] p = line.split(",");

                    if (p.length < 5) {
                        continue;
                    }

                    String sc = clean(p[4]);

                    if (sc.isEmpty()) {
                        continue;
                    }

                    try {

                        int id = Integer.parseInt(clean(p[1]));
                        int score = Integer.parseInt(sc);

                        long[] agg = perStudent.computeIfAbsent(id, k -> new long[2]);
                        agg[0] += score;
                        agg[1]++;

                        if (count == scores.length) {
                            scores = Arrays.copyOf(scores, count * 2);
                        }

                        scores[count++] = score;

                    } catch (NumberFormatException e) {
                        // skip bad row
                    }
                }

            } catch (IOException e) {
                // use what we have
            }
        }

        if (count == 0) {

            // synthetic fallback so the demo still runs
            result.synthetic = true;

            Random rnd = new Random(11);

            for (int s = 0; s < 5000; s++) {

                int n = 3 + rnd.nextInt(8);

                for (int i = 0; i < n; i++) {

                    int score = 30 + rnd.nextInt(71);

                    long[] agg = perStudent.computeIfAbsent(s + 1, k -> new long[2]);
                    agg[0] += score;
                    agg[1]++;

                    if (count == scores.length) {
                        scores = Arrays.copyOf(scores, count * 2);
                    }

                    scores[count++] = score;
                }
            }
        }

        result.allScores = Arrays.copyOf(scores, count);

        int n = perStudent.size();

        result.studentIds = new int[n];
        result.average = new double[n];

        int i = 0;

        for (Map.Entry<Integer, long[]> e : perStudent.entrySet()) {
            result.studentIds[i] = e.getKey();
            result.average[i] = (double) e.getValue()[0] / e.getValue()[1];
            i++;
        }

        return result;
    }

    // =========================================================
    // 1. RANDOMIZED QUICKSORT  (descending, 3-way partition)
    // A random pivot makes the expected time O(n log n) on every
    // input, and the 3-way partition handles many equal averages.
    // =========================================================

    private static long comparisons;

    public static void randomizedQuickSort(double[] key, int[] id, Random rnd) {

        comparisons = 0;

        sort(key, id, 0, key.length - 1, rnd);
    }

    private static void sort(double[] k, int[] id, int lo, int hi, Random rnd) {

        while (lo < hi) {

            int p = lo + rnd.nextInt(hi - lo + 1);
            double pivot = k[p];

            int lt = lo;
            int gt = hi;
            int i = lo;

            while (i <= gt) {

                comparisons++;

                if (k[i] > pivot) {
                    swap(k, id, lt++, i++);
                } else if (k[i] < pivot) {
                    swap(k, id, i, gt--);
                } else {
                    i++;
                }
            }

            // recurse into the smaller side, loop on the larger one
            if (lt - lo < hi - gt) {
                sort(k, id, lo, lt - 1, rnd);
                lo = gt + 1;
            } else {
                sort(k, id, gt + 1, hi, rnd);
                hi = lt - 1;
            }
        }
    }

    private static void swap(double[] k, int[] id, int a, int b) {

        double tk = k[a];
        k[a] = k[b];
        k[b] = tk;

        int ti = id[a];
        id[a] = id[b];
        id[b] = ti;
    }

    private static String quickSortReport(Scores data) {

        StringBuilder sb = new StringBuilder();

        sb.append("1. RANDOMIZED QUICKSORT - RANKING STUDENT RECORDS\n");
        sb.append("------------------------------------------------------------\n");

        int n = data.studentIds.length;

        double[] key = data.average.clone();
        int[] id = data.studentIds.clone();

        long t0 = System.nanoTime();
        randomizedQuickSort(key, id, new Random(2024));
        long qsTime = (System.nanoTime() - t0) / 1000;

        // reference result
        double[] check = data.average.clone();

        t0 = System.nanoTime();
        Arrays.sort(check);
        long refTime = (System.nanoTime() - t0) / 1000;

        boolean sorted = true;

        for (int i = 0; i < n; i++) {
            if (key[i] != check[n - 1 - i]) {
                sorted = false;
                break;
            }
        }

        double nlogn = n * (Math.log(n) / Math.log(2));

        sb.append("Students ranked        : ").append(n).append("\n");
        sb.append("Ranking key            : average assessment score\n");
        sb.append("Comparisons            : ").append(comparisons).append("\n");
        sb.append(String.format("n log2 n               : %.0f  (comparisons / n log2 n = %.2f)%n",
                nlogn, comparisons / nlogn));
        sb.append("Randomized QuickSort   : ").append(qsTime).append(" us\n");
        sb.append("Arrays.sort (reference): ").append(refTime).append(" us\n");
        sb.append("Matches reference order: ").append(sorted ? "YES" : "NO").append("\n\n");

        sb.append("Top 10 students:\n");
        sb.append(String.format("  %-6s %-12s %s%n", "Rank", "Student ID", "Avg score"));

        for (int i = 0; i < Math.min(10, n); i++) {
            sb.append(String.format("  %-6d %-12d %.2f%n", i + 1, id[i], key[i]));
        }

        int tied = 0;

        for (int i = 0; i < n && key[i] == key[0]; i++) {
            tied++;
        }

        sb.append("\n").append(tied).append(" student(s) share the top average of ")
                .append(String.format("%.2f", key[0])).append(" (ties are ordered arbitrarily).\n\n");

        return sb.toString();
    }

    // =========================================================
    // 2. RESERVOIR SAMPLING (Algorithm R)
    // Keeps a uniform random sample of k items from a stream of
    // unknown length using only O(k) memory.
    // Item i (1-based) replaces a random slot with probability k/i.
    // =========================================================

    public static class StreamStats {

        public long rows = 0;
        public long clicks = 0;
        public String[] sample;
        public long[] weekly = new long[WEEKS];
        public boolean synthetic = false;
    }

    // studentVle.csv: code_module,code_presentation,id_student,id_site,date,sum_click
    public static StreamStats streamActivity(File folder, int k, long maxRows, Random rnd) {

        StreamStats stats = new StreamStats();

        stats.sample = new String[k];

        File file = new File(folder, "studentVle.csv");

        if (file.exists()) {

            try (BufferedReader br = new BufferedReader(new FileReader(file), 1 << 16)) {

                br.readLine(); // header

                String line;

                while ((line = br.readLine()) != null && stats.rows < maxRows) {

                    String[] p = line.split(",");

                    if (p.length < 6) {
                        continue;
                    }

                    stats.rows++;

                    // reservoir step
                    if (stats.rows <= k) {
                        stats.sample[(int) stats.rows - 1] = line.replace("\"", "");
                    } else {
                        long j = (long) (rnd.nextDouble() * stats.rows);

                        if (j < k) {
                            stats.sample[(int) j] = line.replace("\"", "");
                        }
                    }

                    // weekly click statistics
                    try {

                        int date = Integer.parseInt(clean(p[4]));
                        int click = Integer.parseInt(clean(p[5]));

                        stats.clicks += click;

                        if (date >= 0 && date / 7 < WEEKS) {
                            stats.weekly[date / 7] += click;
                        }

                    } catch (NumberFormatException e) {
                        // skip bad row
                    }
                }

            } catch (IOException e) {
                // use what we have
            }
        }

        if (stats.rows == 0) {

            // synthetic fallback
            stats.synthetic = true;

            for (int r = 1; r <= 200_000; r++) {

                int date = rnd.nextInt(270);
                int click = 1 + rnd.nextInt(8);
                String line = "SYN,2014J," + (1000 + rnd.nextInt(9000)) + ",0," + date + "," + click;

                stats.rows++;

                if (r <= k) {
                    stats.sample[r - 1] = line;
                } else {
                    long j = (long) (rnd.nextDouble() * r);
                    if (j < k) {
                        stats.sample[(int) j] = line;
                    }
                }

                stats.clicks += click;
                stats.weekly[date / 7] += click;
            }
        }

        return stats;
    }

    private static String reservoirReport(StreamStats stats) {

        StringBuilder sb = new StringBuilder();

        sb.append("2. RESERVOIR SAMPLING - STUDENT ACTIVITY STREAM\n");
        sb.append("------------------------------------------------------------\n");

        sb.append("Rows streamed        : ").append(stats.rows);

        if (stats.synthetic) {
            sb.append("  (synthetic: studentVle.csv not found)");
        } else if (stats.rows >= MAX_VLE_ROWS) {
            sb.append("  (capped at ").append(MAX_VLE_ROWS).append(" for speed)");
        }

        sb.append("\n");
        sb.append("Memory used          : ").append(stats.sample.length).append(" rows (O(k), not O(n))\n");
        sb.append("Total clicks seen    : ").append(stats.clicks).append("\n\n");

        sb.append("Random sample (module, presentation, student, site, day, clicks):\n");

        for (String s : stats.sample) {
            sb.append("  ").append(s).append("\n");
        }

        // uniformity test on a tiny known stream
        int items = 100;
        int k = 10;
        int trials = 20_000;

        int[] hits = new int[items];

        Random rnd = new Random(99);

        for (int t = 0; t < trials; t++) {

            int[] reservoir = new int[k];

            for (int i = 0; i < items; i++) {

                if (i < k) {
                    reservoir[i] = i;
                } else {
                    int j = rnd.nextInt(i + 1);
                    if (j < k) {
                        reservoir[j] = i;
                    }
                }
            }

            for (int r : reservoir) {
                hits[r]++;
            }
        }

        int min = Integer.MAX_VALUE;
        int max = 0;

        for (int h : hits) {
            min = Math.min(min, h);
            max = Math.max(max, h);
        }

        sb.append("\nUniformity check: stream of ").append(items).append(" items, k = ").append(k)
                .append(", ").append(trials).append(" trials\n");
        sb.append("  expected selections per item = ").append(trials * k / items).append("\n");
        sb.append("  observed range               = ").append(min).append(" .. ").append(max).append("\n\n");

        return sb.toString();
    }

    // =========================================================
    // 3. MILLER-RABIN PRIMALITY TEST
    // Write n-1 = 2^r * d (d odd). For each base a, n passes if
    // a^d = 1 or a^(2^i d) = -1 (mod n) for some i < r. These 12
    // prime bases make the test deterministic for n < 3.3 * 10^24,
    // so it is exact for every 64-bit number.
    // =========================================================

    private static final long[] BASES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    public static boolean millerRabin(long n) {

        if (n < 2) {
            return false;
        }

        for (long p : BASES) {

            if (n == p) {
                return true;
            }

            if (n % p == 0) {
                return false;
            }
        }

        long d = n - 1;
        int r = 0;

        while ((d & 1) == 0) {
            d >>= 1;
            r++;
        }

        BigInteger N = BigInteger.valueOf(n);
        BigInteger nMinus1 = N.subtract(BigInteger.ONE);
        BigInteger D = BigInteger.valueOf(d);

        for (long a : BASES) {

            BigInteger x = BigInteger.valueOf(a).modPow(D, N);

            if (x.equals(BigInteger.ONE) || x.equals(nMinus1)) {
                continue;
            }

            boolean composite = true;

            for (int i = 1; i < r; i++) {

                x = x.multiply(x).mod(N);

                if (x.equals(nMinus1)) {
                    composite = false;
                    break;
                }
            }

            if (composite) {
                return false;
            }
        }

        return true;
    }

    private static boolean trialDivision(long n) {

        if (n < 2) {
            return false;
        }

        for (long i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }

        return true;
    }

    private static String millerRabinReport(StreamStats stats, Scores data) {

        StringBuilder sb = new StringBuilder();

        sb.append("3. MILLER-RABIN - PRIMALITY OF LARGE NUMBERS\n");
        sb.append("------------------------------------------------------------\n");

        long[] numbers = {
                561L,                        // Carmichael number (fools Fermat)
                3215031751L,                 // strong pseudoprime to bases 2,3,5,7
                1000000007L,                 // prime
                1000000007L * 998244353L,    // product of two primes
                2305843009213693951L,        // 2^61 - 1 (Mersenne prime)
                2305843009213693953L,        // 2^61 + 1 (divisible by 3)
                stats.clicks,                // taken from the activity stream
                (long) data.allScores.length * 1000003L + 7  // taken from score records
        };

        String[] labels = {
                "Carmichael number",
                "Strong pseudoprime",
                "Known prime 10^9+7",
                "(10^9+7)(998244353)",
                "Mersenne 2^61 - 1",
                "2^61 + 1",
                "Total clicks (stream)",
                "Score records x 1000003 + 7"
        };

        sb.append(String.format("%-28s %22s %-10s %-10s%n", "Number", "Value", "Miller-R", "Verified"));
        sb.append("------------------------------------------------------------\n");

        for (int i = 0; i < numbers.length; i++) {

            boolean mr = millerRabin(numbers[i]);

            String verified;

            if (numbers[i] < 4_000_000_000_000L) {
                verified = (trialDivision(numbers[i]) == mr) ? "agrees" : "DIFFERS";
            } else {
                verified = "n/a (large)";
            }

            sb.append(String.format("%-28s %22d %-10s %-10s%n",
                    labels[i], numbers[i], mr ? "PRIME" : "COMPOSITE", verified));
        }

        // cross-check on all small numbers
        int mismatches = 0;

        for (int n = 0; n < 100_000; n++) {
            if (millerRabin(n) != trialDivision(n)) {
                mismatches++;
            }
        }

        sb.append("\nCross-check on every n < 100000 against trial division: ")
                .append(mismatches == 0 ? "no mismatches" : mismatches + " mismatches")
                .append("\n");
        sb.append("Cost: O(12 * log^2 n) modular multiplications per test,\n");
        sb.append("while trial division needs up to sqrt(n) steps.\n\n");

        return sb.toString();
    }

    // =========================================================
    // 4. BLELLOCH SCAN (work-efficient parallel prefix sum)
    // Up-sweep builds partial sums in a tree, down-sweep pushes
    // prefixes back down.   Work = 2(n-1)   Depth = 2 log2 n
    // =========================================================

    public static long[] blellochExclusiveScan(long[] input, boolean parallel) {

        int n = input.length;

        if (n == 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("length must be a power of two");
        }

        final long[] a = input.clone();

        // ---- up-sweep (reduce) ----
        for (int d = 1; d < n; d *= 2) {

            final int dd = d;
            final int step = 2 * d;
            final int blocks = n / step;

            if (parallel && blocks >= 1024) {
                IntStream.range(0, blocks).parallel().forEach(j -> {
                    int i = j * step;
                    a[i + step - 1] += a[i + dd - 1];
                });
            } else {
                for (int j = 0; j < blocks; j++) {
                    int i = j * step;
                    a[i + step - 1] += a[i + dd - 1];
                }
            }
        }

        a[n - 1] = 0;

        // ---- down-sweep ----
        for (int d = n / 2; d >= 1; d /= 2) {

            final int dd = d;
            final int step = 2 * d;
            final int blocks = n / step;

            if (parallel && blocks >= 1024) {
                IntStream.range(0, blocks).parallel().forEach(j -> {
                    int i = j * step;
                    long t = a[i + dd - 1];
                    a[i + dd - 1] = a[i + step - 1];
                    a[i + step - 1] += t;
                });
            } else {
                for (int j = 0; j < blocks; j++) {
                    int i = j * step;
                    long t = a[i + dd - 1];
                    a[i + dd - 1] = a[i + step - 1];
                    a[i + step - 1] += t;
                }
            }
        }

        return a;
    }

    private static String scanReport(StreamStats stats) {

        StringBuilder sb = new StringBuilder();

        sb.append("4. BLELLOCH SCAN - CUMULATIVE ACADEMIC STATISTICS\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Input : clicks per course week (").append(WEEKS).append(" weeks)\n\n");

        long[] weekly = stats.weekly;
        long[] exclusive = blellochExclusiveScan(weekly, false);

        // inclusive = exclusive + original; compare with sequential
        long running = 0;
        boolean ok = true;

        sb.append(String.format("%-6s %14s %16s%n", "Week", "Clicks", "Cumulative"));
        sb.append("------------------------------------------------------------\n");

        int lastWithData = 0;

        for (int w = 0; w < WEEKS; w++) {
            if (weekly[w] > 0) {
                lastWithData = w;
            }
        }

        for (int w = 0; w < WEEKS; w++) {

            running += weekly[w];

            long inclusive = exclusive[w] + weekly[w];

            if (inclusive != running) {
                ok = false;
            }

            if (w <= lastWithData) {
                sb.append(String.format("%-6d %14d %16d%n", w, weekly[w], inclusive));
            }
        }

        sb.append("\nMatches sequential prefix sum : ").append(ok ? "YES" : "NO").append("\n");
        sb.append("Work  = 2(n-1)  = ").append(2 * (WEEKS - 1)).append(" operations\n");
        sb.append("Depth = 2 log2 n = ").append(2 * (int) (Math.log(WEEKS) / Math.log(2))).append(" steps\n\n");

        // large-scale timing
        int big = 1 << 22;
        long[] data = new long[big];

        for (int i = 0; i < big; i++) {
            data[i] = (i * 2654435761L >>> 12) % 100;
        }

        // warm up
        blellochExclusiveScan(data, true);

        long t0 = System.nanoTime();
        long[] seqScan = new long[big];
        long acc = 0;

        for (int i = 0; i < big; i++) {
            seqScan[i] = acc;
            acc += data[i];
        }

        long seqTime = (System.nanoTime() - t0) / 1000;

        t0 = System.nanoTime();
        long[] parScan = blellochExclusiveScan(data, true);
        long parTime = (System.nanoTime() - t0) / 1000;

        boolean same = Arrays.equals(seqScan, parScan);

        sb.append("Scale test: ").append(big).append(" elements, ")
                .append(Runtime.getRuntime().availableProcessors()).append(" processor(s)\n");
        sb.append("  sequential prefix sum : ").append(seqTime).append(" us\n");
        sb.append("  Blelloch (parallel)   : ").append(parTime).append(" us\n");
        sb.append("  results identical     : ").append(same ? "YES" : "NO").append("\n");
        sb.append("  Blelloch does about 2x the additions of the sequential scan,\n");
        sb.append("  so it only wins when enough processors are available.\n\n");

        return sb.toString();
    }

    // =========================================================
    // 5. PARALLEL REDUCE (fork/join tree reduction)
    // Splits the array in halves, reduces both halves in parallel
    // and combines {sum, max, count}. Work = n-1, Depth = log2 n.
    // =========================================================

    private static class ReduceTask extends RecursiveTask<long[]> {

        private static final long serialVersionUID = 1L;
        private static final int THRESHOLD = 1 << 14;

        private final int[] data;
        private final int lo;
        private final int hi;

        ReduceTask(int[] data, int lo, int hi) {
            this.data = data;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected long[] compute() {

            if (hi - lo <= THRESHOLD) {

                long sum = 0;
                long max = Long.MIN_VALUE;

                for (int i = lo; i < hi; i++) {
                    sum += data[i];
                    max = Math.max(max, data[i]);
                }

                return new long[]{sum, max, hi - lo};
            }

            int mid = (lo + hi) >>> 1;

            ReduceTask left = new ReduceTask(data, lo, mid);
            ReduceTask right = new ReduceTask(data, mid, hi);

            left.fork();

            long[] r = right.compute();
            long[] l = left.join();

            return new long[]{l[0] + r[0], Math.max(l[1], r[1]), l[2] + r[2]};
        }
    }

    public static long[] parallelReduce(int[] data) {
        return ForkJoinPool.commonPool().invoke(new ReduceTask(data, 0, data.length));
    }

    private static long[] sequentialReduce(int[] data) {

        long sum = 0;
        long max = Long.MIN_VALUE;

        for (int v : data) {
            sum += v;
            max = Math.max(max, v);
        }

        return new long[]{sum, max, data.length};
    }

    private static String reduceReport(Scores data) {

        StringBuilder sb = new StringBuilder();

        sb.append("5. PARALLEL REDUCE - STUDENT PERFORMANCE AGGREGATION\n");
        sb.append("------------------------------------------------------------\n");

        long[] seq = sequentialReduce(data.allScores);
        long[] par = parallelReduce(data.allScores);

        sb.append("Assessment results aggregated : ").append(par[2]).append("\n");
        sb.append("Total of all scores           : ").append(par[0]).append("\n");
        sb.append(String.format("Average score                 : %.2f%n", (double) par[0] / par[2]));
        sb.append("Highest score                 : ").append(par[1]).append("\n");
        sb.append("Parallel equals sequential    : ")
                .append(Arrays.equals(seq, par) ? "YES" : "NO").append("\n\n");

        // scale test
        int n = 20_000_000;
        int[] big = new int[n];

        for (int i = 0; i < n; i++) {
            big[i] = (int) (((long) i * 2654435761L >>> 16) % 101);
        }

        // warm up
        sequentialReduce(big);
        parallelReduce(big);

        long t0 = System.nanoTime();
        long[] s = sequentialReduce(big);
        long seqTime = (System.nanoTime() - t0) / 1000;

        t0 = System.nanoTime();
        long[] p = parallelReduce(big);
        long parTime = (System.nanoTime() - t0) / 1000;

        sb.append("Scale test: ").append(n).append(" synthetic scores, ")
                .append(Runtime.getRuntime().availableProcessors()).append(" processor(s)\n");
        sb.append("  sequential : ").append(seqTime).append(" us\n");
        sb.append("  fork/join  : ").append(parTime).append(" us\n");
        sb.append(String.format("  speedup    : %.2fx%n", (double) seqTime / Math.max(1, parTime)));
        sb.append("  identical  : ").append(Arrays.equals(s, p) ? "YES" : "NO").append("\n\n");

        return sb.toString();
    }

    // =========================================================
    // 6. BRENT'S THEOREM
    // A computation with work W (total operations) and depth D
    // (longest dependency chain) runs on p processors in time
    //          T_p <= (W - D) / p + D
    // so speedup is at most W / T_p and a good schedule needs
    // p about W / D processors to stay efficient.
    // =========================================================

    private static String brentReport(Scores data) {

        StringBuilder sb = new StringBuilder();

        sb.append("6. BRENT'S THEOREM - PARALLEL ACADEMIC-DATA PROCESSING\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Bound: T_p <= (W - D) / p + D   (unit-cost operations)\n\n");

        long n = data.allScores.length;

        String[] names = {"Parallel reduce", "Blelloch scan"};
        long[] work = {n - 1, 2 * (n - 1)};

        double log = Math.log(n) / Math.log(2);

        long[] depth = {(long) Math.ceil(log), 2 * (long) Math.ceil(log)};

        int[] processors = {1, 2, 4, 8, 16, 64, 1024};

        for (int a = 0; a < names.length; a++) {

            long w = work[a];
            long d = depth[a];

            sb.append(names[a]).append(" on n = ").append(n).append(" records:\n");
            sb.append("  Work W = ").append(w).append("   Depth D = ").append(d)
                    .append("   parallelism W/D = ").append(w / d).append("\n");

            sb.append(String.format("  %-8s %14s %12s %12s%n", "p", "T_p bound", "speedup", "efficiency"));

            for (int p : processors) {

                double tp = (double) (w - d) / p + d;
                double speedup = (double) w / tp;
                double efficiency = speedup / p;

                sb.append(String.format("  %-8d %14.0f %11.2fx %11.1f%%%n",
                        p, tp, speedup, efficiency * 100));
            }

            sb.append("  Efficiency stays high while p <= W/D = ").append(w / d)
                    .append(" and drops after that.\n\n");
        }

        sb.append("Reading the table: adding processors helps until the depth\n");
        sb.append("D (the log n tree height) dominates; beyond p = W/D the extra\n");
        sb.append("processors sit idle, so speedup flattens at about W/D.\n\n");

        return sb.toString();
    }

    // =========================================================
    // FULL REPORT USED BY THE GUI
    // =========================================================

    public static String runAll(File dataFolder) {

        StringBuilder sb = new StringBuilder();

        sb.append("============================================================\n");
        sb.append("           CO6 - RANDOMIZED AND PARALLEL ALGORITHMS\n");
        sb.append("============================================================\n\n");

        Scores scores = loadScores(dataFolder);

        if (scores.synthetic) {
            sb.append("NOTE: studentAssessment.csv not found - using synthetic scores.\n\n");
        }

        StreamStats stats = streamActivity(dataFolder, 10, MAX_VLE_ROWS, new Random(7));

        sb.append(quickSortReport(scores));
        sb.append(reservoirReport(stats));
        sb.append(millerRabinReport(stats, scores));
        sb.append(scanReport(stats));
        sb.append(reduceReport(scores));
        sb.append(brentReport(scores));

        sb.append("============================================================\n");
        sb.append("Randomized and Parallel algorithms executed successfully.\n");
        sb.append("============================================================\n");

        return sb.toString();
    }
}
