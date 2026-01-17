package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class ExtractCommand extends AbstractCommand implements Holdable {

    public ExtractCommand() {
        setName("extract");
        setSyntax("extract [origin:<origin>] [destination:<destination>] (mode:<mode>) (encoding:<encoding>)");
        setRequiredArguments(2, 4);
        addRemappedPrefixes("origin", "o");
        addRemappedPrefixes("destination", "d");
        addRemappedPrefixes("mode", "m");
        addRemappedPrefixes("encoding", "e");
        autoCompile();
    }

    // <--[command]
    // @Name Extract
    // @Syntax extract [origin:<origin>] [destination:<destination>] (mode:{default}/merge/replace) (encoding:<text>)
    // @Required 2
    // @Maximum 4
    // @Short Extracts an archive to the specified location with a specific strategy.
    // @Group file
    //
    // @Description
    // Extracts the contents of an archive from one location to another.
    // Supports Zip, Tar, GZip, 7z, Cpio, etc.
    //
    // The 'mode' argument controls how conflicts are handled:
    // - 'default': Extracts files. If a file already exists in the destination, it is SKIPPED.
    // - 'merge': Extracts files. If a file already exists, it is OVERWRITTEN.
    // - 'replace': Completely DELETES the destination folder before extraction.
    //
    // The 'encoding' argument allows specifying a character set for filenames (e.g., 'CP866' for Russian Zip files on Windows).
    // If unspecified, auto-detection or UTF-8 is used.
    //
    // @Tags
    // <entry[saveName].success> returns whether the extraction succeeded.
    //
    // @Usage
    // Standard extraction.
    // - ~extract o:data/backup.zip d:plugins/MyPlugin/
    //
    // @Usage
    // Extract a legacy zip archive with specific encoding.
    // - ~extract o:archive.zip d:test/ encoding:CP866
    // -->

    public enum ExtractMode {
        DEFAULT, MERGE, REPLACE
    }

    public static void autoExecute(final ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("origin") final String origin,
                                   @ArgPrefixed @ArgName("destination") final String destination,
                                   @ArgPrefixed @ArgDefaultText("default") @ArgName("mode") final String modeInput,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("encoding") final String encoding) {

        File o = new File(DenizenCore.implementation.getDataFolder(), origin);
        File d = new File(DenizenCore.implementation.getDataFolder(), destination);

        ExtractMode mode;
        try {
            mode = ExtractMode.valueOf(modeInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            Debug.echoError(scriptEntry, "Invalid mode '" + modeInput + "' specified. Valid modes are: DEFAULT, MERGE, REPLACE.");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        // --- Security Checks ---
        if (!DenizenCore.implementation.canReadFile(o)) {
            Debug.echoError(scriptEntry, "Cannot read from origin path due to security settings in Denizen/config.yml.");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        if (!o.exists()) {
            Debug.echoError(scriptEntry, "File extraction failed: Origin file does not exist!");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        if (!DenizenCore.implementation.canWriteToFile(d)) {
            Debug.echoError(scriptEntry, "Cannot write to destination path due to security settings in Denizen/config.yml.");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        final ExtractMode finalMode = mode;

        Runnable runme = () -> {
            try {
                // REPLACE logic: Delete directory recursively
                if (finalMode == ExtractMode.REPLACE && d.exists()) {
                    try (Stream<Path> walk = Files.walk(d.toPath())) {
                        walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                    }
                    if (d.exists()) throw new IOException("Failed to delete existing directory for REPLACE mode: " + d);
                }

                // Create destination directories
                if (!d.exists()) {
                    if (!d.mkdirs() && !d.getParentFile().mkdirs() && !d.getParentFile().exists()) {
                        throw new IOException("Failed to create destination directories.");
                    }
                }

                // Check for 7z Signature
                boolean is7z = false;
                try (InputStream is = new FileInputStream(o)) {
                    byte[] signature = new byte[6];
                    if (is.read(signature) == 6 && SevenZFile.matches(signature, 6)) {
                        is7z = true;
                    }
                } catch (Exception ignored) { }

                if (is7z) {
                    extract7z(o, d, finalMode, scriptEntry);
                } else {
                    extractStream(o, d, finalMode, encoding, scriptEntry);
                }

                scriptEntry.saveObject("success", new ElementTag("true"));
                scriptEntry.setFinished(true);

            } catch (Exception e) {
                String msg = e.getMessage();
                // Specific error handling for locked files (Windows)
                if (msg != null && (msg.contains("Access is denied") || msg.contains("being used by another process"))) {
                    Debug.echoError(scriptEntry, "Extraction failed: Access Denied. The file might be locked by the server or another process (e.g., updating an active plugin jar).");
                } else {
                    Debug.echoError(scriptEntry, "Extraction error: " + msg);
                }

                if (scriptEntry.shouldDebug()) Debug.echoError(scriptEntry, e);
                scriptEntry.saveObject("success", new ElementTag("false"));
                scriptEntry.setFinished(true);
            }
        };

        DenizenCore.runAsync(runme);
    }

    private static void extractStream(File o, File d, ExtractMode mode, String encoding, ScriptEntry scriptEntry) throws Exception {
        try (InputStream fi = Files.newInputStream(o.toPath());
             InputStream bi = new BufferedInputStream(fi)) {

            InputStream in = bi;
            try {
                String type = CompressorStreamFactory.detect(bi);
                in = new BufferedInputStream(new CompressorStreamFactory().createCompressorInputStream(type, bi));
            } catch (CompressorException | IllegalArgumentException e) { }

            ArchiveStreamFactory factory = new ArchiveStreamFactory();
            if (encoding != null) {
                factory.setEntryEncoding(encoding);
            }

            try (ArchiveInputStream ais = factory.createArchiveInputStream(in)) {
                ArchiveEntry entry;
                while ((entry = ais.getNextEntry()) != null) {
                    if (!ais.canReadEntryData(entry)) continue;

                    // Filter macOS metadata garbage
                    String name = entry.getName();
                    if (name.startsWith("__MACOSX") || name.contains("/__MACOSX") || name.contains("\\__MACOSX") ||
                            name.startsWith("._") || name.contains("/._") || name.contains("\\._")) {
                        continue;
                    }

                    handleEntry(d, entry.getName(), entry.isDirectory(), mode, (out) -> {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = ais.read(buffer)) != -1) {
                            out.write(buffer, 0, length);
                        }
                    }, scriptEntry);
                }
            }
        }
    }

    private static void extract7z(File o, File d, ExtractMode mode, ScriptEntry scriptEntry) throws Exception {
        try (SevenZFile sevenZFile = new SevenZFile(o)) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (!entry.hasStream()) continue;

                // Filter macOS metadata garbage
                String name = entry.getName();
                if (name.startsWith("__MACOSX") || name.contains("/__MACOSX") || name.contains("\\__MACOSX") ||
                        name.startsWith("._") || name.contains("/._") || name.contains("\\._")) {
                    continue;
                }

                handleEntry(d, entry.getName(), entry.isDirectory(), mode, (out) -> {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = sevenZFile.read(buffer)) != -1) {
                        out.write(buffer, 0, length);
                    }
                }, scriptEntry);
            }
        }
    }

    private static void handleEntry(File destDir, String entryName, boolean isDirectory, ExtractMode mode, EntryWriter writer, ScriptEntry scriptEntry) throws IOException {
        // --- FILENAME SANITIZATION ---
        // Replaces characters invalid on Windows with underscores to prevent "Bad pathname" errors.
        String cleanName = entryName.replaceAll("[<>:\"|?*]", "_");

        File outFile = new File(destDir, cleanName);

        // Security: Zip Slip Protection
        String canonicalDest = destDir.getCanonicalPath();
        String canonicalOut = outFile.getCanonicalPath();

        if (!canonicalOut.startsWith(canonicalDest)) {
            Debug.echoError(scriptEntry, "Security Warning: Zip Slip attempt detected for entry: '" + cleanName + "'. Skipping.");
            return;
        }

        if (isDirectory) {
            if (!outFile.isDirectory() && !outFile.mkdirs()) throw new IOException("Failed to create directory: " + outFile);
            return;
        }

        // DEFAULT mode: Skip if file exists
        if (mode == ExtractMode.DEFAULT && outFile.exists()) {
            return;
        }

        File parent = outFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Failed to create parent directory: " + parent);

        try (OutputStream out = Files.newOutputStream(outFile.toPath())) {
            writer.write(out);
        }
    }

    @FunctionalInterface
    interface EntryWriter {
        void write(OutputStream out) throws IOException;
    }
}