package Edutrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

/**
 * CO4 - NETWORK FLOW
 *
 * 1. Bipartite Matching   -> assign faculty to eligible courses
 * 2. Ford-Fulkerson (DFS) -> classroom / resource allocation
 * 3. Edmonds-Karp (BFS)   -> academic resource-flow problems
 * 4. Dinic's algorithm    -> larger allocation networks
 * 5. Konig's theorem      -> analysing faculty-course conflicts
 */
public class NetworkFlow {

    // =========================================================
    // FLOW NETWORK (residual graph, edge i and i^1 are a pair)
    // =========================================================

    public static class FlowNetwork {

        private final int n;
        private final List<Integer>[] adj;
        private final List<int[]> endpoints = new ArrayList<>(); // {from, to}
        private final List<Long> cap = new ArrayList<>();
        private final List<Long> original = new ArrayList<>();

        // statistics of the last run
        public int augmentations = 0;

        @SuppressWarnings("unchecked")
        public FlowNetwork(int n) {
            this.n = n;
            adj = new List[n];
            for (int i = 0; i < n; i++) {
                adj[i] = new ArrayList<>();
            }
        }

        public int size() {
            return n;
        }

        // Adds a directed edge u -> v with capacity c and returns its id
        public int addEdge(int u, int v, long c) {

            int id = cap.size();

            endpoints.add(new int[]{u, v});
            cap.add(c);
            original.add(c);
            adj[u].add(id);

            endpoints.add(new int[]{v, u});
            cap.add(0L);
            original.add(0L);
            adj[v].add(id + 1);

            return id;
        }

        // Flow currently sent on the forward edge with this id
        public long flowOn(int edgeId) {
            return original.get(edgeId) - cap.get(edgeId);
        }

        public int edgeTo(int edgeId) {
            return endpoints.get(edgeId)[1];
        }

        public int edgeFrom(int edgeId) {
            return endpoints.get(edgeId)[0];
        }

        public List<Integer> edgesOf(int u) {
            return adj[u];
        }

        // Restore all capacities so another algorithm can run
        public void reset() {
            for (int i = 0; i < cap.size(); i++) {
                cap.set(i, original.get(i));
            }
            augmentations = 0;
        }

        // -----------------------------------------------------
        // FORD-FULKERSON (DFS augmenting paths)
        // Complexity: O(E * maxFlow)
        // -----------------------------------------------------
        public long fordFulkerson(int s, int t) {

            long flow = 0;
            augmentations = 0;

            while (true) {

                boolean[] visited = new boolean[n];

                long pushed = dfsAugment(s, t, Long.MAX_VALUE, visited);

                if (pushed == 0) {
                    break;
                }

                flow += pushed;
                augmentations++;
            }

            return flow;
        }

        private long dfsAugment(int u, int t, long limit, boolean[] visited) {

            if (u == t) {
                return limit;
            }

            visited[u] = true;

            for (int id : adj[u]) {

                int v = endpoints.get(id)[1];
                long c = cap.get(id);

                if (c > 0 && !visited[v]) {

                    long pushed = dfsAugment(v, t, Math.min(limit, c), visited);

                    if (pushed > 0) {
                        cap.set(id, cap.get(id) - pushed);
                        cap.set(id ^ 1, cap.get(id ^ 1) + pushed);
                        return pushed;
                    }
                }
            }

            return 0;
        }

        // -----------------------------------------------------
        // EDMONDS-KARP (BFS = shortest augmenting path)
        // Complexity: O(V * E^2)
        // -----------------------------------------------------
        public long edmondsKarp(int s, int t) {

            long flow = 0;
            augmentations = 0;

            while (true) {

                int[] parentEdge = new int[n];
                Arrays.fill(parentEdge, -1);

                boolean[] seen = new boolean[n];
                Queue<Integer> queue = new ArrayDeque<>();

                queue.add(s);
                seen[s] = true;

                while (!queue.isEmpty() && !seen[t]) {

                    int u = queue.poll();

                    for (int id : adj[u]) {

                        int v = endpoints.get(id)[1];

                        if (!seen[v] && cap.get(id) > 0) {
                            seen[v] = true;
                            parentEdge[v] = id;
                            queue.add(v);
                        }
                    }
                }

                if (!seen[t]) {
                    break;
                }

                // bottleneck on the path
                long bottleneck = Long.MAX_VALUE;
                int v = t;

                while (v != s) {
                    int id = parentEdge[v];
                    bottleneck = Math.min(bottleneck, cap.get(id));
                    v = endpoints.get(id)[0];
                }

                // push flow
                v = t;

                while (v != s) {
                    int id = parentEdge[v];
                    cap.set(id, cap.get(id) - bottleneck);
                    cap.set(id ^ 1, cap.get(id ^ 1) + bottleneck);
                    v = endpoints.get(id)[0];
                }

                flow += bottleneck;
                augmentations++;
            }

            return flow;
        }

        // -----------------------------------------------------
        // DINIC (level graph + blocking flow)
        // Complexity: O(V^2 * E)
        // -----------------------------------------------------
        private int[] level;
        private int[] next;

        public long dinic(int s, int t) {

            long flow = 0;
            augmentations = 0; // counts phases (level graphs) here

            while (buildLevels(s, t)) {

                next = new int[n];

                long pushed;

                while ((pushed = blockingFlow(s, t, Long.MAX_VALUE)) > 0) {
                    flow += pushed;
                }

                augmentations++;
            }

            return flow;
        }

        private boolean buildLevels(int s, int t) {

            level = new int[n];
            Arrays.fill(level, -1);

            Queue<Integer> queue = new ArrayDeque<>();

            queue.add(s);
            level[s] = 0;

            while (!queue.isEmpty()) {

                int u = queue.poll();

                for (int id : adj[u]) {

                    int v = endpoints.get(id)[1];

                    if (level[v] < 0 && cap.get(id) > 0) {
                        level[v] = level[u] + 1;
                        queue.add(v);
                    }
                }
            }

            return level[t] >= 0;
        }

        private long blockingFlow(int u, int t, long limit) {

            if (u == t) {
                return limit;
            }

            for (; next[u] < adj[u].size(); next[u]++) {

                int id = adj[u].get(next[u]);
                int v = endpoints.get(id)[1];

                if (cap.get(id) > 0 && level[v] == level[u] + 1) {

                    long pushed = blockingFlow(v, t, Math.min(limit, cap.get(id)));

                    if (pushed > 0) {
                        cap.set(id, cap.get(id) - pushed);
                        cap.set(id ^ 1, cap.get(id ^ 1) + pushed);
                        return pushed;
                    }
                }
            }

            return 0;
        }
    }

    // =========================================================
    // BIPARTITE MATCHING (Kuhn's augmenting paths)
    // left  = faculty, right = courses
    // =========================================================

    public static class BipartiteMatching {

        public final int leftSize;
        public final int rightSize;
        public final List<List<Integer>> graph = new ArrayList<>();

        public int[] matchLeft;   // matchLeft[u]  = right vertex or -1
        public int[] matchRight;  // matchRight[v] = left vertex or -1

        public BipartiteMatching(int leftSize, int rightSize) {

            this.leftSize = leftSize;
            this.rightSize = rightSize;

            for (int i = 0; i < leftSize; i++) {
                graph.add(new ArrayList<>());
            }
        }

        public void addEdge(int left, int right) {
            graph.get(left).add(right);
        }

        public int maximumMatching() {

            matchLeft = new int[leftSize];
            matchRight = new int[rightSize];

            Arrays.fill(matchLeft, -1);
            Arrays.fill(matchRight, -1);

            int result = 0;

            for (int u = 0; u < leftSize; u++) {

                boolean[] visited = new boolean[rightSize];

                if (tryAugment(u, visited)) {
                    result++;
                }
            }

            return result;
        }

        private boolean tryAugment(int u, boolean[] visited) {

            for (int v : graph.get(u)) {

                if (visited[v]) {
                    continue;
                }

                visited[v] = true;

                if (matchRight[v] == -1 || tryAugment(matchRight[v], visited)) {
                    matchLeft[u] = v;
                    matchRight[v] = u;
                    return true;
                }
            }

            return false;
        }

        // -----------------------------------------------------
        // KONIG'S THEOREM
        // In a bipartite graph:
        //   size(max matching) = size(min vertex cover)
        //   max independent set = V - min vertex cover
        //
        // Construction (run after maximumMatching):
        //   Z = vertices reachable from unmatched LEFT vertices
        //       by alternating paths (non-matching edge L->R,
        //       matching edge R->L)
        //   min vertex cover = (L \ Z) U (R intersect Z)
        // -----------------------------------------------------
        public boolean[][] minimumVertexCover() {

            boolean[] visitedLeft = new boolean[leftSize];
            boolean[] visitedRight = new boolean[rightSize];

            for (int u = 0; u < leftSize; u++) {
                if (matchLeft[u] == -1 && !visitedLeft[u]) {
                    alternate(u, visitedLeft, visitedRight);
                }
            }

            boolean[] coverLeft = new boolean[leftSize];
            boolean[] coverRight = new boolean[rightSize];

            for (int u = 0; u < leftSize; u++) {
                coverLeft[u] = !visitedLeft[u];
            }

            for (int v = 0; v < rightSize; v++) {
                coverRight[v] = visitedRight[v];
            }

            return new boolean[][]{coverLeft, coverRight};
        }

        private void alternate(int u, boolean[] visitedLeft, boolean[] visitedRight) {

            visitedLeft[u] = true;

            for (int v : graph.get(u)) {

                if (visitedRight[v] || matchLeft[u] == v) {
                    continue;
                }

                visitedRight[v] = true;

                int w = matchRight[v];

                if (w != -1 && !visitedLeft[w]) {
                    alternate(w, visitedLeft, visitedRight);
                }
            }
        }
    }

    // =========================================================
    // DATA HELPERS
    // =========================================================

    private static String clean(String s) {
        return s.replace("\"", "").trim();
    }

    // Reads courses.csv -> list of "MODULE-PRESENTATION"
    public static List<String> loadCourses(File folder) {

        List<String> courses = new ArrayList<>();

        File file = new File(folder, "courses.csv");

        if (!file.exists()) {
            return courses;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",");

                if (parts.length >= 2) {
                    courses.add(clean(parts[0]) + "-" + clean(parts[1]));
                }
            }

        } catch (IOException e) {
            // return whatever was read
        }

        return courses;
    }

    // Reads studentInfo.csv -> number of students per course presentation
    public static Map<String, Integer> loadEnrollment(File folder) {

        Map<String, Integer> counts = new TreeMap<>();

        File file = new File(folder, "studentInfo.csv");

        if (!file.exists()) {
            return counts;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine(); // header

            while ((line = br.readLine()) != null) {

                String[] parts = line.split(",", 3);

                if (parts.length >= 2) {
                    String key = clean(parts[0]) + "-" + clean(parts[1]);
                    counts.merge(key, 1, Integer::sum);
                }
            }

        } catch (IOException e) {
            // return whatever was read
        }

        return counts;
    }

    // =========================================================
    // DEMO 1: FACULTY -> COURSE MATCHING + KONIG
    // =========================================================

    private static final String[] FACULTY = {
            "Dr. Rao", "Dr. Iyer", "Dr. Khan",
            "Dr. Mehta", "Dr. Singh", "Dr. Das"
    };

    // Faculty eligibility by course module (one module = one course)
    private static final String[][] ELIGIBLE = {
            {"AAA", "BBB"},   // Rao
            {"BBB", "CCC"},   // Iyer
            {"CCC"},          // Khan
            {"CCC", "DDD"},   // Mehta
            {"EEE"},          // Singh
            {"EEE", "FFF"}    // Das
    };

    private static final String[] MODULES = {
            "AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG"
    };

    private static int moduleIndex(String m) {
        for (int i = 0; i < MODULES.length; i++) {
            if (MODULES[i].equals(m)) {
                return i;
            }
        }
        return -1;
    }

    public static String facultyMatchingReport() {

        StringBuilder sb = new StringBuilder();

        BipartiteMatching bm = new BipartiteMatching(FACULTY.length, MODULES.length);

        sb.append("1. BIPARTITE MATCHING - FACULTY TO COURSES\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Eligibility (faculty -> modules):\n");

        for (int i = 0; i < FACULTY.length; i++) {

            sb.append(String.format("  %-10s : ", FACULTY[i]));

            for (int k = 0; k < ELIGIBLE[i].length; k++) {

                bm.addEdge(i, moduleIndex(ELIGIBLE[i][k]));

                sb.append(ELIGIBLE[i][k]);

                if (k < ELIGIBLE[i].length - 1) {
                    sb.append(", ");
                }
            }

            sb.append("\n");
        }

        int size = bm.maximumMatching();

        sb.append("\nMaximum matching size : ").append(size).append("\n\n");
        sb.append("Assignments:\n");

        for (int i = 0; i < FACULTY.length; i++) {

            sb.append(String.format("  %-10s -> %s%n",
                    FACULTY[i],
                    bm.matchLeft[i] == -1 ? "(no course)" : MODULES[bm.matchLeft[i]]));
        }

        List<String> unassigned = new ArrayList<>();

        for (int v = 0; v < MODULES.length; v++) {
            if (bm.matchRight[v] == -1) {
                unassigned.add(MODULES[v]);
            }
        }

        sb.append("\nCourses without faculty : ")
                .append(unassigned.isEmpty() ? "none" : unassigned)
                .append("\n\n");

        // ---------------- KONIG ----------------

        sb.append("2. KONIG'S THEOREM - FACULTY/COURSE CONFLICT ANALYSIS\n");
        sb.append("------------------------------------------------------------\n");

        boolean[][] cover = bm.minimumVertexCover();

        List<String> coverNames = new ArrayList<>();
        List<String> independent = new ArrayList<>();

        for (int i = 0; i < FACULTY.length; i++) {
            (cover[0][i] ? coverNames : independent).add(FACULTY[i]);
        }

        for (int v = 0; v < MODULES.length; v++) {
            (cover[1][v] ? coverNames : independent).add(MODULES[v]);
        }

        sb.append("Minimum vertex cover (touches every eligibility link):\n");
        sb.append("  ").append(coverNames).append("\n");
        sb.append("  size = ").append(coverNames.size()).append("\n\n");

        sb.append("Maximum independent set (no eligibility link inside it):\n");
        sb.append("  ").append(independent).append("\n");
        sb.append("  size = ").append(independent.size()).append("\n\n");

        sb.append("Check: max matching (").append(size)
                .append(") = min vertex cover (").append(coverNames.size())
                .append(") -> ")
                .append(size == coverNames.size() ? "Konig's theorem holds" : "MISMATCH")
                .append("\n");

        sb.append("Check: |V| - cover = independent set -> ")
                .append(FACULTY.length + MODULES.length)
                .append(" - ").append(coverNames.size())
                .append(" = ").append(independent.size()).append("\n");

        if (!unassigned.isEmpty()) {
            sb.append("Conflict insight: course ").append(unassigned)
                    .append(" has no eligible faculty; adding an eligible\n");
            sb.append("faculty member for it is the only way to raise the matching.\n");
        }

        sb.append("\n");

        return sb.toString();
    }

    // =========================================================
    // DEMO 2: CLASSROOM ALLOCATION (FF, EK, DINIC)
    //
    // source -> course presentation (capacity = enrolled students)
    //        -> room (only if the room is available to that course)
    //        -> sink (capacity = room seats)
    // =========================================================

    private static final String[] ROOMS = {
            "Hall-A", "Hall-B", "Hall-C", "Lab-1", "Lab-2", "Seminar-1"
    };

    private static final double[] ROOM_SHARE = {
            0.22, 0.20, 0.18, 0.15, 0.12, 0.08
    };

    public static String classroomAllocationReport(File folder) {

        StringBuilder sb = new StringBuilder();

        List<String> courses = loadCourses(folder);
        Map<String, Integer> enrollment = loadEnrollment(folder);

        // fallback if data is missing
        if (courses.isEmpty()) {
            courses = Arrays.asList("AAA-2013J", "AAA-2014J", "BBB-2013J", "BBB-2014J");
        }

        int[] demand = new int[courses.size()];
        long totalDemand = 0;

        Random fallback = new Random(7);

        for (int i = 0; i < courses.size(); i++) {

            Integer count = enrollment.get(courses.get(i));

            demand[i] = (count != null) ? count : 200 + fallback.nextInt(600);

            totalDemand += demand[i];
        }

        // room seats = share of total demand (a deliberate bottleneck)
        int[] seats = new int[ROOMS.length];

        for (int r = 0; r < ROOMS.length; r++) {
            seats[r] = (int) Math.round(totalDemand * ROOM_SHARE[r]);
        }

        // node ids: 0 = source, 1..C = courses, C+1..C+R = rooms, last = sink
        int C = courses.size();
        int R = ROOMS.length;

        int source = 0;
        int sink = C + R + 1;

        FlowNetwork net = new FlowNetwork(C + R + 2);

        for (int i = 0; i < C; i++) {
            net.addEdge(source, 1 + i, demand[i]);
        }

        List<int[]> courseRoomEdges = new ArrayList<>(); // {edgeId, course, room}

        for (int i = 0; i < C; i++) {

            for (int r = 0; r < R; r++) {

                // availability rule: some rooms are unavailable per course
                if ((i + r) % 3 != 0) {
                    int id = net.addEdge(1 + i, 1 + C + r, demand[i]);
                    courseRoomEdges.add(new int[]{id, i, r});
                }
            }
        }

        for (int r = 0; r < R; r++) {
            net.addEdge(1 + C + r, sink, seats[r]);
        }

        sb.append("3. CLASSROOM / RESOURCE ALLOCATION - MAX FLOW\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Course presentations : ").append(C).append("\n");
        sb.append("Rooms                : ").append(R).append("\n");
        sb.append("Total student demand : ").append(totalDemand).append("\n");
        sb.append("Total room seats     : ").append(Arrays.stream(seats).sum()).append("\n\n");

        // ---- run the three algorithms on the same network ----

        sb.append(String.format("%-16s %12s %16s %12s%n",
                "Algorithm", "Max flow", "Augment/Phases", "Time (us)"));

        sb.append("------------------------------------------------------------\n");

        net.reset();
        long t0 = System.nanoTime();
        long ff = net.fordFulkerson(source, sink);
        long ffTime = (System.nanoTime() - t0) / 1000;
        int ffAug = net.augmentations;

        net.reset();
        t0 = System.nanoTime();
        long ek = net.edmondsKarp(source, sink);
        long ekTime = (System.nanoTime() - t0) / 1000;
        int ekAug = net.augmentations;

        net.reset();
        t0 = System.nanoTime();
        long dn = net.dinic(source, sink);
        long dnTime = (System.nanoTime() - t0) / 1000;
        int dnPhases = net.augmentations;

        sb.append(String.format("%-16s %12d %16d %12d%n", "Ford-Fulkerson", ff, ffAug, ffTime));
        sb.append(String.format("%-16s %12d %16d %12d%n", "Edmonds-Karp", ek, ekAug, ekTime));
        sb.append(String.format("%-16s %12d %16d %12d%n", "Dinic", dn, dnPhases, dnTime));

        sb.append("\nAll three agree : ").append(ff == ek && ek == dn ? "YES" : "NO").append("\n\n");

        // net currently holds Dinic's final flow -> read the allocation
        sb.append("Allocation (Dinic result): students placed per room\n");

        long[] perRoom = new long[R];

        Map<String, StringBuilder> perCourse = new LinkedHashMap<>();

        long[] placed = new long[C];

        for (int[] e : courseRoomEdges) {

            long f = net.flowOn(e[0]);

            perRoom[e[2]] += f;
            placed[e[1]] += f;
        }

        for (int r = 0; r < R; r++) {

            double pct = seats[r] == 0 ? 0 : 100.0 * perRoom[r] / seats[r];

            sb.append(String.format("  %-10s %6d / %-6d seats  (%.1f%% full)%n",
                    ROOMS[r], perRoom[r], seats[r], pct));
        }

        sb.append("\nCourses not fully seated (unmet demand):\n");

        boolean any = false;

        for (int i = 0; i < C; i++) {

            long unmet = demand[i] - placed[i];

            if (unmet > 0) {
                any = true;
                sb.append(String.format("  %-12s needs %5d, placed %5d, short by %d%n",
                        courses.get(i), demand[i], placed[i], unmet));
            }
        }

        if (!any) {
            sb.append("  none - every course is fully seated\n");
        }

        sb.append("\nTotal seated : ").append(dn).append(" of ").append(totalDemand)
                .append(String.format(" (%.1f%%)%n", 100.0 * dn / totalDemand));

        sb.append("\n");

        return sb.toString();
    }

    // =========================================================
    // DEMO 3: SCALE TEST - why Dinic is used for larger networks
    // =========================================================

    public static String scaleReport() {

        StringBuilder sb = new StringBuilder();

        int layers = 4;
        int width = 150;
        int nodes = layers * width + 2;

        int source = 0;
        int sink = nodes - 1;

        FlowNetwork net = new FlowNetwork(nodes);

        Random rnd = new Random(42);

        // source -> layer 0
        for (int i = 0; i < width; i++) {
            net.addEdge(source, 1 + i, 20 + rnd.nextInt(30));
        }

        // layer k -> layer k+1 (random sparse edges)
        for (int k = 0; k < layers - 1; k++) {

            for (int i = 0; i < width; i++) {

                for (int e = 0; e < 8; e++) {

                    int u = 1 + k * width + i;
                    int v = 1 + (k + 1) * width + rnd.nextInt(width);

                    net.addEdge(u, v, 1 + rnd.nextInt(20));
                }
            }
        }

        // last layer -> sink
        for (int i = 0; i < width; i++) {
            net.addEdge(1 + (layers - 1) * width + i, sink, 20 + rnd.nextInt(30));
        }

        sb.append("4. LARGE ALLOCATION NETWORK - ALGORITHM COMPARISON\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("Generated network: ").append(nodes).append(" nodes, ")
                .append(layers * width * 8).append("+ edges\n\n");

        sb.append(String.format("%-16s %12s %16s %12s%n",
                "Algorithm", "Max flow", "Augment/Phases", "Time (us)"));

        sb.append("------------------------------------------------------------\n");

        net.reset();
        long t0 = System.nanoTime();
        long ff = net.fordFulkerson(source, sink);
        long ffTime = (System.nanoTime() - t0) / 1000;
        int ffAug = net.augmentations;

        net.reset();
        t0 = System.nanoTime();
        long ek = net.edmondsKarp(source, sink);
        long ekTime = (System.nanoTime() - t0) / 1000;
        int ekAug = net.augmentations;

        net.reset();
        t0 = System.nanoTime();
        long dn = net.dinic(source, sink);
        long dnTime = (System.nanoTime() - t0) / 1000;
        int dnPhases = net.augmentations;

        sb.append(String.format("%-16s %12d %16d %12d%n", "Ford-Fulkerson", ff, ffAug, ffTime));
        sb.append(String.format("%-16s %12d %16d %12d%n", "Edmonds-Karp", ek, ekAug, ekTime));
        sb.append(String.format("%-16s %12d %16d %12d%n", "Dinic", dn, dnPhases, dnTime));

        sb.append("\nAll three agree : ").append(ff == ek && ek == dn ? "YES" : "NO").append("\n");
        sb.append("Note: Dinic finishes in a few phases because each phase\n");
        sb.append("saturates a whole level graph instead of one path.\n\n");

        return sb.toString();
    }

    // =========================================================
    // FULL REPORT USED BY THE GUI
    // =========================================================

    public static String runAll(File dataFolder) {

        StringBuilder sb = new StringBuilder();

        sb.append("============================================================\n");
        sb.append("                  CO4 - NETWORK FLOW\n");
        sb.append("============================================================\n\n");

        sb.append(facultyMatchingReport());
        sb.append(classroomAllocationReport(dataFolder));
        sb.append(scaleReport());

        sb.append("============================================================\n");
        sb.append("Network Flow algorithms executed successfully.\n");
        sb.append("============================================================\n");

        return sb.toString();
    }
}
