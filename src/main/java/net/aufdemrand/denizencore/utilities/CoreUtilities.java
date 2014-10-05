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
}
