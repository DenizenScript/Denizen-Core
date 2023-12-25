package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WebGetCommand extends AbstractCommand implements Holdable {

    public WebGetCommand() {
        setName("webget");
        setSyntax("webget [<url>] (data:<data>) (method:<method>) (headers:<map>) (timeout:<duration>/{10s}) (savefile:<path>) (hide_failure)");
        setRequiredArguments(1, 7);
        isProcedural = false;
        autoCompile();
        addRemappedPrefixes("data", "post");
        addRemappedPrefixes("timeout", "t");
    }

    // <--[command]
    // @Name Webget
    // @Syntax webget [<url>] (data:<data>) (method:<method>) (headers:<map>) (timeout:<duration>/{10s}) (savefile:<path>) (hide_failure)
    // @Required 1
    // @Maximum 7
    // @Short Gets the contents of a web page or API response.
    // @Synonyms wget
    // @Group core
    //
    // @Description
    // Connects to a webpage or API and downloads its contents, to be used via the save argument and corresponding entry tags.
    //
    // This should almost always be ~waited for. Refer to <@link language ~waitable>.
    //
    // Note that while this will replace URL spaces to %20, you are responsible for any other necessary URL encoding.
    // You may want to use the <@link tag ElementTag.url_encode> tag for this.
    //
    // Optionally, use "data:<data>" to specify a set of data to send to the server (changes the default method from GET to POST).
    // A BinaryTag input will be used directly - any other input will be treated as a String and encoded as UTF-8.
    //
    // Optionally, use "method:<method>" to specify the HTTP method to use in your request.
    // Can be: GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE, PATCH.
    //
    // Optionally, use "headers:" to specify a MapTag of headers.
    //
    // Optionally, use "savefile:" to specify a path to save the retrieved file to.
    // This will remove the 'result' entry savedata.
    // Path is relative to server base directory.
    //
    // Optionally, specify the "timeout:" to set how long the command should wait for a webpage to load before giving up. Defaults to 10 seconds.
    //
    // Optionally, specify 'hide_failure' to indicate that connection errors are acceptable and shouldn't display in logs.
    //
    // This command accepts secret inputs via <@link ObjectType SecretTag> as the URL or as the value of any header.
    // Note that you cannot mix secret with non-secret - meaning, "webget <secret[my_secret]>" and "webget https://example.com" are both valid, but "webget https://example.com/<secret[my_secret]>" is not.
    // Similarly, for headers, each individual header value can either be a secret or not a secret.
    //
    // @Tags
    // <entry[saveName].failed> returns whether the webget failed. A failure occurs when the status is not 2XX/3XX or webget failed to connect.
    // <entry[saveName].result> returns the text of the result of the webget. This is null only if webget failed to connect to the url.
    // <entry[saveName].result_binary> returns the raw binary data of the result of the webget. This is null only if webget failed to connect to the url.
    // <entry[saveName].result_headers> returns a MapTag of the headers returned from the webserver. Every value in the result is a list.
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

    public enum Method { GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE, PATCH }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("url") @ArgRaw ElementTag originalUrl,
                                   @ArgPrefixed @ArgName("data") @ArgDefaultNull ObjectTag data,
                                   @ArgPrefixed @ArgName("method") @ArgDefaultNull Method method,
                                   @ArgName("hide_failure") boolean hideFailure,
                                   @ArgPrefixed @ArgName("timeout") @ArgDefaultText("10s") DurationTag timeout,
                                   @ArgPrefixed @ArgName("headers") @ArgDefaultNull ObjectTag headersLegacyCompat,
                                   @ArgPrefixed @ArgName("savefile") @ArgDefaultNull String saveFile) {
        MapTag headers = null;
        if (headersLegacyCompat != null && headersLegacyCompat.shouldBeType(MapTag.class)) {
            headers = headersLegacyCompat.asType(MapTag.class, scriptEntry.context);
        }
        else if (headersLegacyCompat != null) {
            headers = new MapTag();
            for (String str : headersLegacyCompat.asType(ListTag.class, scriptEntry.context)) {
                int ind = str.indexOf('/');
                if (ind > 0) {
                    headers.putObject(str.substring(0, ind), new ElementTag(str.substring(ind + 1)));
                }
            }
        }
        if (!CoreConfiguration.allowWebget) {
            Debug.echoError(scriptEntry, "WebGet disabled in config.yml!");
            return;
        }
        byte[] actualData = null;
        if (data != null) {
            if (data.shouldBeType(BinaryTag.class)) {
                actualData = data.asType(BinaryTag.class, scriptEntry.context).data;
            }
            else {
                actualData = data.identify().getBytes(StandardCharsets.UTF_8);
            }
        }
        // Secrets processing
        String urlText = originalUrl.asString();
        final boolean urlIsSecret = originalUrl.canBeType(SecretTag.class);
        if (urlIsSecret) {
            SecretTag secret = originalUrl.asType(SecretTag.class, scriptEntry.context);
            if (secret == null) {
                Debug.echoError("Invalid URL SecretTag object '" + originalUrl.asString() + "' - secret not defined in 'secrets.secret'?");
                return;
            }
            urlText = secret.getValue();
        }
        MapTag newHeaders = null;
        if (headers != null) {
            newHeaders = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : headers.entrySet()) {
                ObjectTag value = entry.getValue();
                if (value.canBeType(SecretTag.class)) {
                    SecretTag secret = value.asType(SecretTag.class, scriptEntry.context);
                    if (secret == null) {
                        Debug.echoError("Invalid header SecretTag object '" + value + "' - secret not defined in 'secrets.secret'?");
                        return;
                    }
                    value = new ElementTag(secret.getValue(), true);
                }
                newHeaders.putObject(entry.getKey(), value);
            }
        }
        final MapTag headersFinal = newHeaders;
        final String urlFinal = urlText;
        final byte[] finalData = actualData;
        // Actual execution
        if (!urlFinal.startsWith("http://") && !urlFinal.startsWith("https://")) {
            Debug.echoError("Must have a valid (HTTP/HTTPS) URL! Attempted: " + originalUrl.asString()); // Note: use original url for error, in case of secret input
            return;
        }
        Thread thr = new Thread(() -> webGet(scriptEntry, finalData, method, urlFinal, timeout, headersFinal, saveFile, hideFailure, urlIsSecret));
        thr.start();
    }

    public static void writeToFile(InputStream in, String saveFile) throws Exception {
        File file = new File(saveFile);
        if (!DenizenCore.implementation.canWriteToFile(file)) {
            Debug.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
            return;
        }
        FileOutputStream fout = new FileOutputStream(file);
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            fout.write(buffer, 0, len);
        }
        fout.flush();
        fout.close();
    }

    public static boolean patchAlreadyPatched = false;

    /**
     * This fixes the methods field in HttpURLConnection class to support the PATCH method.
     * I named this fix method this way because the name deserves to be as stupid as the concept.
     */
    public static void patchPatchMethodMethodsField() {
        if (patchAlreadyPatched) {
            return;
        }
        patchAlreadyPatched = true;
        try {
            ReflectionHelper.giveReflectiveAccess(HttpURLConnection.class, WebGetCommand.class);
            String[] methods = ReflectionHelper.getFieldValue(HttpURLConnection.class, "methods", null);
            String[] outMethods = new String[methods.length + 1];
            System.arraycopy(methods, 0, outMethods, 0, methods.length);
            outMethods[methods.length] = "PATCH";
            ReflectionHelper.getFinalSetter(HttpURLConnection.class, "methods").invoke(outMethods);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    public static void webGet(final ScriptEntry scriptEntry, final byte[] data, Method method, String urlText, DurationTag timeout, MapTag headers, String saveFile, boolean hideFailure, boolean urlIsSecret) {
        HttpURLConnection uc = null;
        try {
            long timeStart = CoreUtilities.monotonicMillis();
            URL url = new URL(urlText.replace(" ", "%20"));
            uc = (HttpURLConnection) url.openConnection();
            uc.setDoInput(true);
            uc.setDoOutput(true);
            if (method != null) {
                if (method == Method.PATCH) {
                    patchPatchMethodMethodsField();
                }
                uc.setRequestMethod(method.name());
            }
            else if (data != null) {
                uc.setRequestMethod("POST");
            }
            if (headers != null) {
                for (Map.Entry<StringHolder, ObjectTag> pair : headers.entrySet()) {
                    uc.setRequestProperty(pair.getKey().str, pair.getValue().toString());
                }
            }
            uc.setConnectTimeout((int) timeout.getMillis());
            uc.connect();
            if (data != null) {
                uc.getOutputStream().write(data);
            }
            final int status = uc.getResponseCode();
            byte[] result = null;
            if (saveFile != null) {
                writeToFile(uc.getInputStream(), saveFile);
            }
            else {
                InputStream stream = uc.getInputStream();
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = stream.read(buffer, 0, 1024)) != -1) {
                    bytesOut.write(buffer, 0, len);
                }
                result = bytesOut.toByteArray();
                bytesOut.close();
                stream.close();
            }
            final byte[] outResult = result;
            MapTag resultHeaders = new MapTag();
            for (Map.Entry<String, List<String>> header : uc.getHeaderFields().entrySet()) {
                String key = header.getKey();
                if (key == null) {
                    key = "null";
                }
                resultHeaders.putObject(key, new ListTag(header.getValue(), true));
            }
            final long timeDone = CoreUtilities.monotonicMillis();
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.saveObject("status", new ElementTag(status));
                    scriptEntry.saveObject("failed", new ElementTag(status >= 200 && status < 400 ? "false" : "true"));
                    if (saveFile == null) {
                        if (outResult != null) {
                            scriptEntry.saveObject("result", new ElementTag(new String(outResult, StandardCharsets.UTF_8)));
                            scriptEntry.saveObject("result_binary", new BinaryTag(outResult));
                        }
                        scriptEntry.saveObject("result_headers", resultHeaders);
                    }
                    scriptEntry.saveObject("time_ran", new DurationTag((timeDone - timeStart) / 1000.0));
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
        catch (Exception e) {
            if (!hideFailure || uc == null) {
                if (urlIsSecret) {
                    Debug.echoError("WebGet encountered an exception of type '" + e.getClass().getCanonicalName() + "' but hid the exception text due to secret URL presence.");
                }
                else {
                    Debug.echoError(e);
                }
            }
            int tempStatus = -1;
            byte[] result = null;
            if (uc != null) {
                try {
                    tempStatus = uc.getResponseCode();
                    InputStream errorStream = uc.getErrorStream();
                    if (errorStream != null) {
                        if (saveFile != null) {
                            writeToFile(errorStream, saveFile);
                        }
                        else {
                            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = errorStream.read(buffer, 0, 1024)) != -1) {
                                bytesOut.write(buffer, 0, len);
                            }
                            result = bytesOut.toByteArray();
                            bytesOut.close();
                            errorStream.close();
                        }
                    }
                }
                catch (Exception e2) {
                    if (!hideFailure) {
                        if (urlIsSecret) {
                            Debug.echoError("WebGet encountered an exception of type '" + e2.getClass().getCanonicalName() + "' but hid the exception text due to secret URL presence.");
                        }
                        else {
                            Debug.echoError(e2);
                        }
                    }
                }
            }
            final byte[] outResult = result;
            final int status = tempStatus;
            DenizenCore.schedule(new Schedulable() {
                @Override
                public boolean tick(float seconds) {
                    scriptEntry.saveObject("failed", new ElementTag("true"));
                    if (status != -1) {
                        scriptEntry.saveObject("status", new ElementTag(status));
                        if (saveFile == null) {
                            if (outResult != null) {
                                scriptEntry.saveObject("result", new ElementTag(new String(outResult, StandardCharsets.UTF_8)));
                                scriptEntry.saveObject("result_binary", new BinaryTag(outResult));
                            }
                        }
                    }
                    scriptEntry.setFinished(true);
                    return false;
                }
            });
        }
    }
}
