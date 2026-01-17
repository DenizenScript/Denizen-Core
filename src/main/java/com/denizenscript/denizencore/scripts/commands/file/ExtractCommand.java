package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.nio.file.Files;

public class ExtractCommand extends AbstractCommand implements Holdable {

    public ExtractCommand() {
        setName("extract");
        setSyntax("extract [origin:<origin>] [destination:<destination>] (overwrite)");
        setRequiredArguments(2, 3);
        addRemappedPrefixes("origin", "o");
        addRemappedPrefixes("destination", "d");
        autoCompile();
    }

    // <--[command]
    // @Name Extract
    // @Syntax extract [origin:<origin>] [destination:<destination>] (overwrite)
    // @Required 2
    // @Maximum 3
    // @Short Extracts an archive (zip, tar, tar.gz, etc.) to the specified location.
    // @Group file
    //
    // @Description
    // Extracts the contents of an archive from one location to another.
    // Supports various formats automatically (Zip, Tar, GZip, 7z, Cpio, etc.) by detecting the file signature.
    //
    // If a destination file already exists, it can be overwritten with the "overwrite" argument.
    //
    // @Tags
    // <entry[saveName].success> returns whether the extraction succeeded.
    //
    // @Usage
    // Use to extract an archive to a destination folder.
    // - ~extract o:data/backup.tar.gz d:data/restored/ overwrite save:extract
    // - narrate "Extraction success<&co> <entry[extract].success>"
    // -->

    public static void autoExecute(final ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("origin") final String origin,
                                   @ArgPrefixed @ArgName("destination") final String destination,
                                   @ArgName("overwrite") final boolean overwrite) {

        File o = new File(DenizenCore.implementation.getDataFolder(), origin);
        File d = new File(DenizenCore.implementation.getDataFolder(), destination);

        boolean dexists = d.exists();
        boolean disdir = d.isDirectory() || destination.endsWith("/");

        if (!DenizenCore.implementation.canReadFile(o)) {
            Debug.echoError(scriptEntry, "Cannot read from that file path due to security settings in Denizen/config.yml.");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        if (!o.exists()) {
            Debug.echoError(scriptEntry, "File extraction failed, origin does not exist!");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        if (!DenizenCore.implementation.canWriteToFile(d)) {
            Debug.echoError(scriptEntry, "Cannot write to that destination path due to security settings in Denizen/config.yml.");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        if (dexists && !disdir && !overwrite) {
            Debug.echoDebug(scriptEntry, "File extraction ignored, destination file already exists!");
            scriptEntry.saveObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }

        Runnable runme = () -> {
            try {
                if (disdir && !dexists) {
                    if (!d.mkdirs()) throw new IOException("Failed to create destination directory.");
                } else if (!dexists && !d.getParentFile().exists()) {
                    if (!d.getParentFile().mkdirs()) throw new IOException("Failed to create parent directory.");
                }

                try (InputStream fi = Files.newInputStream(o.toPath());
                     InputStream bi = new BufferedInputStream(fi)) {

                    InputStream compressorInputStream = bi;

                    try {
                        String compressorType = CompressorStreamFactory.detect(bi);
                        compressorInputStream = new CompressorStreamFactory()
                                .createCompressorInputStream(compressorType, bi);
                        compressorInputStream = new BufferedInputStream(compressorInputStream);
                    } catch (CompressorException e) {
                        // the file is not compressed (for example, a regular .zip or .tar without gz), we continue as is
                    } catch (Exception e) {
                        // ignore compressor detection errors and try opening it as an archive.
                    }

                    try (ArchiveInputStream ais = new ArchiveStreamFactory()
                            .createArchiveInputStream(compressorInputStream)) {

                        ArchiveEntry entry;
                        while ((entry = ais.getNextEntry()) != null) {
                            if (!ais.canReadEntryData(entry)) {
                                continue;
                            }

                            File outFile = new File(d, entry.getName());

                            String canonicalDest = d.getCanonicalPath();
                            String canonicalOut = outFile.getCanonicalPath();
                            if (!canonicalOut.startsWith(canonicalDest)) {
                                Debug.echoError(scriptEntry, "Security Warning: Zip Slip attempt detected! Entry '"
                                        + entry.getName() + "' tries to write outside target folder. Skipping.");
                                continue;
                            }

                            if (entry.isDirectory()) {
                                if (!outFile.isDirectory() && !outFile.mkdirs()) {
                                    throw new IOException("Failed to create directory " + outFile);
                                }
                            } else {
                                File parent = outFile.getParentFile();
                                if (!parent.isDirectory() && !parent.mkdirs()) {
                                    throw new IOException("Failed to create directory " + parent);
                                }

                                Files.copy(ais, outFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }

                scriptEntry.saveObject("success", new ElementTag("true"));
                scriptEntry.setFinished(true);

            } catch (Exception e) {
                Debug.echoError(scriptEntry, "Extraction error: " + e.getMessage());
                if (scriptEntry.shouldDebug()) {
                    Debug.echoError(scriptEntry, e);
                }
                scriptEntry.saveObject("success", new ElementTag("false"));
                scriptEntry.setFinished(true);
            }
        };

        if (scriptEntry.shouldWaitFor()) {
            DenizenCore.runAsync(runme);
        } else {
            runme.run();
        }
    }
}