package net.aufdemrand.denizencore.utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CoreUtilities {

    static Random random = new Random();

    public static Random getRandom() {
        return random;
    }

    protected static FilenameFilter scriptsFilter;

    static {
        scriptsFilter = new FilenameFilter() {
            public boolean accept(File file, String fileName) {
                if(fileName.startsWith(".")) return false;

                String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
                return ext.equalsIgnoreCase("YML") || ext.equalsIgnoreCase("DSCRIPT");
            }
        };
    }

    /**
     * Lists all files in the given directory.
     *
     * @param dir The directory to search in
     * @return A {@link java.io.File} collection
     */
    public static List<File> listDScriptFiles(File dir) {
        List<File> files = new ArrayList<File>();
        File[] entries = dir.listFiles();

        for (File file : entries) {
            // Add file
            if (scriptsFilter == null || scriptsFilter.accept(dir, file.getName())) {
                files.add(file);
            }

            // Add subdirectories
            if (file.isDirectory()) {
                files.addAll(listDScriptFiles(file));
            }
        }
        return files;
    }

    public static List<String> Split(String str, char c) {
        List<String> strings = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                strings.add(str.substring(start, i));
                start = i + 1;
            }
        }
        strings.add(str.substring(start, str.length()));
        return strings;
    }

    public static String toLowerCase(String input) {
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            if (data[i] >= 'A' && data[i] <= 'Z') {
                switch (data[i]) {
                    case 'A': data[i] = 'a'; break;
                    case 'B': data[i] = 'b'; break;
                    case 'C': data[i] = 'c'; break;
                    case 'D': data[i] = 'd'; break;
                    case 'E': data[i] = 'e'; break;
                    case 'F': data[i] = 'f'; break;
                    case 'G': data[i] = 'g'; break;
                    case 'H': data[i] = 'h'; break;
                    case 'I': data[i] = 'i'; break;
                    case 'J': data[i] = 'j'; break;
                    case 'K': data[i] = 'k'; break;
                    case 'L': data[i] = 'l'; break;
                    case 'M': data[i] = 'm'; break;
                    case 'N': data[i] = 'n'; break;
                    case 'O': data[i] = 'o'; break;
                    case 'P': data[i] = 'p'; break;
                    case 'Q': data[i] = 'q'; break;
                    case 'R': data[i] = 'r'; break;
                    case 'S': data[i] = 's'; break;
                    case 'T': data[i] = 't'; break;
                    case 'U': data[i] = 'u'; break;
                    case 'V': data[i] = 'v'; break;
                    case 'W': data[i] = 'w'; break;
                    case 'X': data[i] = 'x'; break;
                    case 'Y': data[i] = 'y'; break;
                    case 'Z': data[i] = 'z'; break;
                }
            }
        }
        return new String(data);
    }
}
