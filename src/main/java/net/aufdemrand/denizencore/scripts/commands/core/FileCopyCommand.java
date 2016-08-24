package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.scripts.commands.Holdable;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.scheduling.Schedulable;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class FileCopyCommand extends AbstractCommand implements Holdable {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("origin")
                    && arg.matchesPrefix("origin", "o")) {
                scriptEntry.addObject("origin", new Element(arg.getValue()));
            }

            else if (!scriptEntry.hasObject("destination")
                    && arg.matchesPrefix("destination", "d")) {
                scriptEntry.addObject("destination", new Element(arg.getValue()));
            }

            else if (!scriptEntry.hasObject("overwrite")
                    && arg.matches("overwrite")) {
                scriptEntry.addObject("overwrite", new Element(true));
            }

            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("origin")) {
            throw new InvalidArgumentsException("Must have a valid origin!");
        }

        if (!scriptEntry.hasObject("destination")) {
            throw new InvalidArgumentsException("Must have a valid destination!");
        }

        scriptEntry.defaultObject("overwrite", new Element(false));
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) throws CommandExecutionException {

        if (!DenizenCore.getImplementation().allowedToFilecopy()) {
            dB.echoError(scriptEntry.getResidingQueue(), "FileCopy disabled by config!");
            return;
        }

        final Element origin = scriptEntry.getElement("origin");
        final Element destination = scriptEntry.getElement("destination");
        final Element overwrite = scriptEntry.getElement("overwrite");

        dB.report(scriptEntry, getName(), origin.debug() + destination.debug() + overwrite.debug());

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                fileCopy(scriptEntry, origin, destination, overwrite);
            }
        });
        thr.start();
    }

    private static final CopyOption[] OVERWRITE_OPTION = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };
    private static final CopyOption[] NO_OPTIONS = new CopyOption[0];

    public void fileCopy(final ScriptEntry scriptEntry, Element origin, Element destination, Element overwrite) {
        boolean success = true;
        try {
            File startDirectory = DenizenCore.getImplementation().getScriptFolder().getParentFile();
            File originFile = new File(startDirectory, origin.asString());
            File destinationFile = new File(startDirectory, destination.asString());
            if (destinationFile.isDirectory()) {
                if (!destinationFile.exists()) {
                    destinationFile.mkdirs();
                }
                Files.copy(originFile.toPath(), destinationFile.toPath().resolve(originFile.getName()),
                        overwrite.asBoolean() ? OVERWRITE_OPTION : NO_OPTIONS);
            }
            else {
                File destParent = destinationFile.getParentFile();
                if (!destParent.exists()) {
                    destParent.mkdirs();
                }
                Files.copy(originFile.toPath(), new File(destination.asString()).toPath(),
                        overwrite.asBoolean() ? OVERWRITE_OPTION : NO_OPTIONS);
            }
        }
        catch (IOException e) {
            success = false;
            dB.echoError(e);
        }
        finally {
            final boolean finalSuccess = success;
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.addObject("success", new Element(finalSuccess));
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
    }
}
