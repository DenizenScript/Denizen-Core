package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.scheduling.RepeatingSchedulable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DebugSubmitter extends Thread {

    /**
     * Available for Denizen implementation and addons to add more lines to debug log submission headers.
     */
    public static List<Supplier<String>> debugHeaderLines = new ArrayList<>();

    public static Supplier<String> pasteTitleGetter = () -> "Denizen Debug Recording From A " + DenizenCore.implementation.getImplementationName() + " Impl";

    public static final String corePasteURL = "https://paste.denizenscript.com/New/Log";

    public static String pasteURL = corePasteURL;

    public String title, prefixContent, recording, result;

    public static void init() {
        debugHeaderLines.add(() -> "Java Version: " + System.getProperty("java.version"));
        debugHeaderLines.add(() -> "Up-time: " + new DurationTag((CoreUtilities.monotonicMillis() - DenizenCore.startTime) / 1000.0).formatted(false));
        debugHeaderLines.add(() -> "Denizen Version: Core: " + DenizenCore.VERSION + ", " + DenizenCore.implementation.getImplementationName() + ": " + DenizenCore.implementation.getImplementationVersion());
        debugHeaderLines.add(() -> "Script Containers: " + ScriptRegistry.scriptContainers.size() + ", Events: " + ScriptEvent.totalPaths);
        debugHeaderLines.add(() -> "Last reload: " + new DurationTag((CoreUtilities.monotonicMillis() - DenizenCore.lastReloadTime) / 1000.0).formatted(false) + " ago");
    }

    public static void submitCurrentRecording(Consumer<String> processResult) {
        if (!CoreConfiguration.shouldRecordDebug || !CoreConfiguration.debugRecordingAllowed) {
            processResult.accept("disabled");
            return;
        }
        CoreConfiguration.shouldRecordDebug = false;
        final DebugSubmitter submit = new DebugSubmitter();
        submit.recording = Debug.debugRecording.toString();
        Debug.debugRecording = new StringBuilder();
        submit.build();
        submit.start();
        RepeatingSchedulable schedulable = new RepeatingSchedulable(null, 0.25f);
        schedulable.run = () -> {
            if (!submit.isAlive()) {
                schedulable.cancel();
                processResult.accept(submit.result);
            }
        };
        DenizenCore.schedule(schedulable);
    }

    public void build() {
        recording = Debug.debugRecording.toString();
        title = pasteTitleGetter.get();
        StringBuilder addedLines = new StringBuilder();
        for (Supplier<String> line : debugHeaderLines) {
            try {
                addedLines.append(line.get()).append('\n');
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
        prefixContent = addedLines.toString();
    }

    @Override
    public void run() {
        BufferedReader in = null;
        try {
            // Open a connection to the paste server
            URL url = new URL(pasteURL);
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            uc.setConnectTimeout(10000);
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uc.connect();
            // Safely connected at this point
            // Create the final message pack and upload it
            uc.getOutputStream().write(("pastetype=log"
                    + "&response=micro&v=200&pastetitle=" + URLEncoder.encode(title, "UTF-8")
                    + "&pastecontents=" + URLEncoder.encode(prefixContent + "\n", "UTF-8") + recording).getBytes(StandardCharsets.UTF_8));
            // Wait for a response from the server
            in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            // Record the response
            result = in.readLine();
            if (result != null && result.startsWith(("<!DOCTYPE html"))) {
                result = null;
            }
            // Close the connection
            in.close();
        }
        catch (Exception e) {
            Debug.echoError(e);
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }
    }
}
