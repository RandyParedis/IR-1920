package helper;

import java.io.File;
import java.util.*;

/**
 * The {@code Helper} class is a class that has a set of static functions that are useful
 * in any other class and thus used globally.
 */
public class Helper {
    /**
     * Transforms an input string into a lowercase, alphanumeric string.
     * @param old   The input string.
     * @return  The transformed string.
     */
    public static String transform(String old) {
        return old.strip().toLowerCase().replaceAll("[^a-z0-9 ]", "");
    }

    /**
     * Loads filenames from a directory into memory, recursively.
     * @param directory The directory to load.
     * @return  An {@code ArrayList<String>} containing all filenames in the directory.
     */
    public static ArrayList<String> loadFiles(String directory) {
        ArrayList<String> res = new ArrayList<>();
        File dir = new File(directory);

        // Iterate via Strings for efficiency
        String[] fls = dir.list();
        for(String file: Objects.requireNonNull(fls)) {
            if(file.equals(".DS_Store")) { continue; } // MAC
            if(file.endsWith(".xml")) { // We're only interested in helper.XML
                res.add(directory + "/" + file);
            } else {
                File f = new File(directory + "/" + file);
                if(f.isDirectory()) {
                    res.addAll(loadFiles(directory + "/" + file));
                }
            }
        }

        return res;
    }

    /**
     * Picks {@code count} filenames from a list, according to a RNG with seed {@code seed}.
     * @param list      The list to pick the filenames from.
     * @param count     The amount of files to pick.
     * @param seed      The seed of the RNG to pick with.
     * @return  An {@code ArrayList<File>} that contains all the {@code File} objects
     *          referring to the picked filenames.
     */
    public static ArrayList<File> pickFiles(ArrayList<String> list, int count, int seed) {
        ArrayList<File> rres = new ArrayList<>();
        System.out.println("Total Dataset Size: " + list.size());

        // Pick <count> Documents (efficiency)
        if(list.size() < count) {
            for(String s : list) {
                rres.add(new File(s));
            }
        } else { // Pick Random
            Random rand = new Random(seed);
            HashSet<Integer> found = new HashSet<>();
            for(int i = 0; i < count; ++i) {
                int n;
                do {
                    n = rand.nextInt(list.size());
                } while(found.contains(n));
                found.add(n);
                rres.add(new File(list.get(n)));
            }
        }

        Collections.sort(rres); // Set the order
        return rres;
    }
}
