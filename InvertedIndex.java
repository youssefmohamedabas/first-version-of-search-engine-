import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class Posting {
    public Posting next = null;
    public int docId;
    public int dtf = 1;
}

class DictEntry {
    public int doc_freq = 0;
    public int term_freq = 0;
    public Posting pList = null;
}

public class InvertedIndex {
    private static final int NUM_FILES = 10; // Number of files to process

    public static void main(String[] args) {
        // Create the inverted index
        HashMap<String, DictEntry> index = buildInvertedIndex();

        // Process user queries
        while (true) {
            System.out.println("- Enter 1 to search for a word");
            System.out.println("- Enter 0 to exit the program");
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();

            if (choice == 1) {
                System.out.print("Enter a word to search for: ");
                Scanner inputScanner = new Scanner(System.in);
                String query = inputScanner.nextLine();
                searchQuery(index, query);
            } else {
                break;
            }
        }
    }

    private static HashMap<String, DictEntry> buildInvertedIndex() {
        HashMap<String, DictEntry> index = new HashMap<>();

        for (int i = 1; i <= NUM_FILES; i++) {
            String fileName = "file" + i + ".txt";
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] words = line.split("[\\s,.]+");
                    for (String word : words) {
                        word = word.toLowerCase();
                        if (!index.containsKey(word)) {
                            index.put(word, new DictEntry());
                        }

                        DictEntry entry = index.get(word);
                        if (entry.pList == null || entry.pList.docId != i) {
                            Posting posting = new Posting();
                            posting.docId = i;
                            posting.next = entry.pList;
                            entry.pList = posting;
                            entry.doc_freq++;
                        } else {
                            entry.pList.dtf++;
                        }
                        entry.term_freq++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return index;
    }

    private static void searchQuery(Map<String, DictEntry> index, String query) {
        String[] queryWords = query.split("\\s+");
        for (String word : queryWords) {
            word = word.toLowerCase();
            if (index.containsKey(word)) {
                DictEntry entry = index.get(word);
                Posting posting = entry.pList;
                while (posting != null) {
                    System.out.println("File " + posting.docId + " contains the word \"" + word + "\"");
                    posting = posting.next;
                }
            } else {
                System.out.println("The word \"" + word + "\" does not appear in any of the files.");
            }
        }
        computeCosineSimilarity(index, query);
    }

    private static void computeCosineSimilarity(Map<String, DictEntry> index, String query) {
        double[] scores = new double[NUM_FILES];
        String[] terms = query.split("\\P{Alpha}+");

        for (String term : terms) {
            term = term.toLowerCase();
            if (index.containsKey(term)) {
                DictEntry entry = index.get(term);
                int docFreq = entry.doc_freq;
                double idf = Math.log10((double) NUM_FILES / docFreq);

                Posting posting = entry.pList;
                while (posting != null) {
                    scores[posting.docId - 1] += (1 + Math.log10((double) posting.dtf)) * idf;
                    posting = posting.next;
                }
            }
        }

        normalizeScores(scores);
        rankFiles(scores);
    }

    private static void normalizeScores(double[] scores) {
        double norm = 0;
        for (double score : scores) {
            norm += score * score;
        }
        norm = Math.sqrt(norm);

        if (norm != 0) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= norm;
            }
        }
    }

    private static void rankFiles(double[] scores) {
        double[] sortedScores = scores.clone();
        Arrays.sort(sortedScores);
        for (int i = sortedScores.length - 1; i >= 0; i--) {
            if (sortedScores[i] > 0) {
                int rank = sortedScores.length - i;
                System.out.println("File " + rank + ".txt: cosine similarity (" + sortedScores[i] + ")");
            }
        }
    }
}