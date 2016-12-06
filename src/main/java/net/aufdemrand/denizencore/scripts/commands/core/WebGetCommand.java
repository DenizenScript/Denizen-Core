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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebGetCommand extends AbstractCommand implements Holdable {
    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("url")) {
                scriptEntry.addObject("url", new Element(arg.raw_value));
            }

            else if (!scriptEntry.hasObject("post")
                    && arg.matchesPrefix("post")) {
                scriptEntry.addObject("post", arg.asElement());
            }

            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("url")) {
            throw new InvalidArgumentsException("Must have a valid URL!");
        }

        Element url = scriptEntry.getElement("url");
        if (!url.asString().startsWith("http://") && !url.asString().startsWith("https://")) {
            throw new InvalidArgumentsException("Must have a valid (HTTP/HTTPS) URL! Attempted: " + url.asString());
        }

    }


    @Override
    public void execute(final ScriptEntry scriptEntry) throws CommandExecutionException {

        if (!DenizenCore.getImplementation().allowedToWebget()) {
            dB.echoError(scriptEntry.getResidingQueue(), "WebGet disabled by config!");
            return;
        }

        final Element url = scriptEntry.getElement("url");

        final String postData = scriptEntry.hasObject("post") ? scriptEntry.getElement("post").asString() : null;

        dB.report(scriptEntry, getName(), url.debug());

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                webGet(scriptEntry, postData, url);
            }
        });
        thr.start();
    }

    public void webGet(final ScriptEntry scriptEntry, final String postData, Element urlp) {

        BufferedReader in = null;
        try {
            URL url = new URL(urlp.asString().replace(" ", "%20"));
            final HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            if (postData != null) {
                uc.setRequestMethod("POST");
            }
            uc.setConnectTimeout(10000); // TODO: Option for this!
            uc.connect();
            if (postData != null) {
                uc.getOutputStream().write(postData.getBytes("UTF-8"));
            }
            in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            // Probably a better way to do this bit.
            while (true) {
                try {
                    String temp = in.readLine();
                    if (temp == null) {
                        break;
                    }
                    sb.append(temp);
                }
                catch (Exception ex) {
                    break;
                }
            }
            in.close();
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    try {
                        scriptEntry.addObject("failed", new Element(uc.getResponseCode() == 200 ? "false" : "true"));
                    }
                    catch (Exception e) {
                        dB.echoError(e);
                    }
                    scriptEntry.addObject("result", new Element(sb.toString()));
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
        catch (Exception e) {
            dB.echoError(e);
            try {
                DenizenCore.schedule(new Schedulable() {
                    @Override
                    public boolean tick(float seconds) {
                        scriptEntry.addObject("failed", new Element("true"));
                        scriptEntry.setFinished(true);
                        return false;
                    }
                });
            }
            catch (Exception e2) {
                dB.echoError(e2);
            }
        }
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (Exception e) {
                dB.echoError(e);
            }
        }
    }
}
