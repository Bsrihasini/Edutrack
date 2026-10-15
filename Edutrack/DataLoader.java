package Edutrack;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    private List<String> records = new ArrayList<>();

    // Loads one record from each non-empty line
    public boolean loadDataset(String filePath) {

        records.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (!line.isEmpty()) {
                    records.add(line);
                }
            }

            System.out.println();
            System.out.println("Dataset loaded successfully.");
            System.out.println("Records loaded: " + records.size());

            return true;

        } catch (IOException e) {

            System.out.println();
            System.out.println("ERROR: Could not load dataset.");
            System.out.println("File path: " + filePath);
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public List<String> getRecords() {
        return records;
    }

    public int getRecordCount() {
        return records.size();
    }

    public String getRecord(int index) {

        if (index < 0 || index >= records.size()) {
            return null;
        }

        return records.get(index);
    }

    // Displays records neatly
    public void displayRecords(int limit) {

        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                           DATASET RECORDS");
        System.out.println("================================================================================");

        int count = Math.min(limit, records.size());

        for (int i = 0; i < count; i++) {

            System.out.println();
            System.out.println("Record #" + (i + 1));
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(records.get(i));
        }

        System.out.println();
        System.out.println("Showing " + count + " of " + records.size() + " records.");
    }
}