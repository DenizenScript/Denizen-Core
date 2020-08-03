package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebGetCommand extends AbstractCommand implements Holdable {

    public WebGetCommand() {
        setName("webget");
        setSyntax("webget [<url>] (post:<data>) (headers:<header>/<value>|...) (timeout:<duration>/{10s}) (savefile:<path>)");
        setRequiredArguments(1, 6);
        isProcedural = false;
    }

    // <--[command]
    // @Name Webget
    // @Syntax webget [<url>] (data:<data>) (method:<method>) (headers:<header>/<value>|...) (timeout:<duration>/{10s}) (savefile:<path>)
    // @Required 1
    // @Maximum 6
    // @Short Gets the contents of a web page or API response.
    // @Group core
    //
    // @Description
    // Connects to a webpage or API and downloads its contents, to be used via the save argument and corresponding entry tags.
    //
    // This should almost always be ~waited for.
    //
    // Note that while this will replace URL spaces to %20, you are responsible for any other necessary URL encoding.
    // You may want to use the <@link tag ElementTag.url_encode> tag for this.
    //
    // Optionally, use "data:<data>" to specify a set of data to send to the server (changes the default method from GET to POST).
    //
    // Optionally, use "method:<method>" to specify the HTTP method to use in your request.
    // Can be: GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE.
    //
    // Optionally, use "headers:" to specify a MapTag of headers.
    //
    // Optionally, use "savefile:" to specify a path to save the retrieved file to.
    // This will remove the 'result' entry savedata.
    // Path is relative to server base directory.
    //
    // Specify the "timeout:" to set how long the command should wait for a webpage to load before giving up. Defaults to 10 seconds.
    //
    // @Tags
    // <entry[saveName].failed> returns whether the webget failed. A failure occurs when the status is not 2XX/3XX or webget failed to connect.
    // <entry[saveName].result> returns the result of the webget. This is null only if webget failed to connect to the url.
    // <entry[saveName].status> returns the HTTP status code of the webget. This is null only if webget failed to connect to the url.
    // <entry[saveName].time_ran> returns a DurationTag indicating how long the web connection processing took.
    // <ElementTag.url_encode>
    //
    // @Usage
    // Use to download the google home page.
    // - ~webget https://google.com save:google
    // - narrate <entry[google].result>
    //
    // @Usage
    // Use to save a webpage to your server's base directory
    // - ~webget https://google.com savefile:google.html
    //
    // @Usage
    // Use to post data to a server.
    // - ~webget https://api.mojang.com/orders/statistics 'data:{"metricKeys":["item_sold_minecraft"]}' headers:<map.with[Content-Type].as[application/json]> save:request
    // - narrate <entry[request].result>
    //
    // @Usage
    // Use to retrieve and load an API response into yaml.
    // - ~webget https://api.mojang.com/users/profiles/minecraft/<player.name> save:request
    // - yaml loadtext:<entry[request].result> id:player_data
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("url")) {
                scriptEntry.addObject("url", new ElementTag(arg.raw_value));
            }
            else if (!scriptEntry.hasObject("data")
                    && arg.matchesPrefix("data", "post")) {
                scriptEntry.addObject("data", arg.asElement());
            }
            else if (!scriptEntry.hasObject("method")
                    && arg.matchesPrefix("method")
                    && arg.matches("get", "post", "head", "options", "put", "delete", "trace")) {
                scriptEntry.addObject("method", arg.asElement());
            }
            else if (!scriptEntry.hasObject("timeout")
                    && arg.matchesPrefix("timeout", "t")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("timeout", arg.asType(DurationTag.class));
            }
            else if (!scriptEntry.hasObject("headers")
                    && arg.matchesPrefix("headers")
                    && arg.getValue().startsWith("map@")) {
                scriptEntry.addObject("headers", arg.asType(MapTag.class));
            }
            else if (!scriptEntry.hasObject("headers")
                    && arg.matchesPrefix("headers")) {
                MapTag map = new MapTag();
                for (String str : arg.asType(ListTag.class)) {
                    int ind = str.indexOf('/');
                    if (ind > 0) {
                        map.putObject(str.substring(0, ind), new ElementTag(str.substring(ind + 1)));
                    }
                }
                scriptEntry.addObject("headers", map);
            }
            else if (!scriptEntry.hasObject("savefile")
                    && arg.matchesPrefix("savefile")) {
                scriptEntry.addObject("savefile", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("url")) {
            throw new InvalidArgumentsException("Must have a valid URL!");
        }
        ElementTag url = scriptEntry.getElement("url");
        if (!url.asString().startsWith("http://") && !url.asString().startsWith("https://")) {
            throw new InvalidArgumentsException("Must have a valid (HTTP/HTTPS) URL! Attempted: " + url.asString());
        }
        scriptEntry.defaultObject("timeout", new DurationTag(10));
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        if (!DenizenCore.getImplementation().allowedToWebget()) {
            Debug.echoError(scriptEntry.getResidingQueue(), "WebGet disabled by config!");
            return;
        }
        final ElementTag url = scriptEntry.getElement("url");
        final ElementTag data = scriptEntry.getElement("data");
        final ElementTag method = scriptEntry.getElement("method");
        final DurationTag timeout = scriptEntry.getObjectTag("timeout");
        final MapTag headers = scriptEntry.getObjectTag("headers");
        final ElementTag saveFile = scriptEntry.getElement("savefile");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), url.debug()
                            + (data != null ? data.debug() : "")
                            + (method != null ? method.debug() : "")
                            + (timeout != null ? timeout.debug() : "")
                            + (saveFile != null ? saveFile.debug() : "")
                            + (headers != null ? headers.debug() : ""));
        }
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                webGet(scriptEntry, data, method, url, timeout, headers, saveFile);
            }
        });
        thr.start();
    }

    public void buildResult(BufferedReader buffIn, StringBuilder sb) {
        // Probably a better way to do this bit.
        while (true) {
            try {
                String temp = buffIn.readLine();
                if (temp == null) {
                    break;
                }
                sb.append(temp).append("\n");
            }
            catch (Exception ex) {
                break;
            }
        }
    }

    public void writeToFile(InputStream in, ElementTag saveFile) throws Exception {
        File file = new File(saveFile.asString());
        if (!DenizenCore.getImplementation().canWriteToFile(file)) {
            Debug.echoError("Cannot write to that file, as dangerous file paths have been disabled in the Denizen config.");
        }
        else {
            FileOutputStream fout = new FileOutputStream(file);
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                fout.write(buffer, 0, len);
            }
            fout.flush();
            fout.close();
        }
    }

    public void webGet(final ScriptEntry scriptEntry, final ElementTag data, ElementTag method, ElementTag urlp, DurationTag timeout, MapTag headers, ElementTag saveFile) {
        BufferedReader buffIn = null;
        HttpURLConnection uc = null;
        try {
            long timeStart = System.currentTimeMillis();
            URL url = new URL(urlp.asString().replace(" ", "%20"));
            uc = (HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            if (method != null) {
                uc.setRequestMethod(method.asString().toUpperCase());
            }
            else if (data != null) {
                uc.setRequestMethod("POST");
            }
            if (headers != null) {
                for (Map.Entry<StringHolder, ObjectTag> pair : headers.map.entrySet()) {
                    uc.setRequestProperty(pair.getKey().str, pair.getValue().toString());
                }
            }
            uc.setConnectTimeout((int) timeout.getMillis());
            uc.connect();
            if (data != null) {
                uc.getOutputStream().write(data.asString().getBytes(StandardCharsets.UTF_8));
            }
            final int status = uc.getResponseCode();
            final StringBuilder sb = new StringBuilder();
            if (saveFile != null) {
                writeToFile(uc.getInputStream(), saveFile);
            }
            else {
                buffIn = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                buildResult(buffIn, sb);
                buffIn.close();
                buffIn = null;
            }
            final long timeDone = System.currentTimeMillis();
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.addObject("status", new ElementTag(status));
                    scriptEntry.addObject("failed", new ElementTag(status >= 200 && status < 400 ? "false" : "true"));
                    if (saveFile == null) {
                        scriptEntry.addObject("result", new ElementTag(sb.toString()));
                    }
                    scriptEntry.addObject("time_ran", new DurationTag((timeDone - timeStart) / 1000.0));
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
        catch (Exception e) {
            int tempStatus = -1;
            final StringBuilder sb = new StringBuilder();
            if (uc != null) {
                try {
                    tempStatus = uc.getResponseCode();
                    InputStream errorStream = uc.getErrorStream();
                    if (errorStream != null) {
                        if (saveFile != null) {
                            writeToFile(errorStream, saveFile);
                        }
                        else {
                            buffIn = new BufferedReader(new InputStreamReader(errorStream));
                            buildResult(buffIn, sb);
                            buffIn.close();
                            buffIn = null;
                        }
                    }
                }
                catch (Exception e2) {
                    Debug.echoError(e2);
                }
            }
            else {
                Debug.echoError(e);
            }
            final int status = tempStatus;
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.addObject("failed", new ElementTag("true"));
                    if (status != -1) {
                        scriptEntry.addObject("status", new ElementTag(status));
                        if (saveFile == null) {
                            scriptEntry.addObject("result", new ElementTag(sb.toString()));
                        }
                    }
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
        finally {
            try {
                if (buffIn != null) {
                    buffIn.close();
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }
    }
}
