package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.scripts.commands.core.WebServerCommand;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebserverWebRequestScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // webserver web request
    //
    // @Switch port:<#> to only handle requests to a specific port.
    // @Switch path:<path> to only handle requests that match the given advanced-matcher for the path.
    // @Switch method:<method> to only handle requests with the specific method (such as GET or POST).
    // @Switch has_response:<true/false> to only handle requests that do or don't have a response already.
    //
    // @Group Core
    //
    // @Triggers when a webserver opened by <@link command webserver> receives a connection request.
    //
    // @Context
    // <context.method> returns the method that was used (such as GET or POST).
    // <context.path> returns the path requested (such as "/index.html").
    // <context.port> returns the port connected to.
    // <context.remote_address> returns the IP address that connected.
    // <context.query> returns a MapTag of the query data (if no query, returns empty map).
    // <context.raw_query> returns the raw query input (if no query, returns null).
    // <context.raw_user_info> returns the raw user info input (if any) (this is a historical HTTP system that allows sending username/password over query).
    // <context.headers> returns a MapTag of all input headers, where the key is the header name and the value is a ListTag of header values for that name.
    // <context.body> returns the content body that was sent, if any. Particularly for POST requests.
    // <context.has_response> returns true if a response body determination (raw_text_content, file, or cached_file) was applied, or false if not.
    //
    // @Determine
    // "CODE:" + Element(Number), to set a standard web response code, such as 'code:200' for 'OK', or 'code:404' for 'File Not Found'
    // "HEADERS": + MapTag to set a map of headers, where map keys are the header name and map values are the text of the value, for example headers:[Content-Type=text/html] ... note that header are sometimes case-sensitive.
    // "RAW_TEXT_CONTENT:" + ElementTag to set a raw text content body in response. You may determine only one response - raw text, a file, or a cached file. You cannot use multiple.
    // "FILE:" + ElementTag to set a path to a file to send in response. File path must be within the web-root path configured in Denizen/config.yml. Files will be read async.
    // "CACHED_FILE:" + ElementTag to set a path to a file to send in response. The content of the file will be cached in RAM until the server restarts. This is useful for files that definitely won't change. First file read will be sync, all others are instant.
    //
    // @Example
    // # This example supplies a manual response to any of the "/", "/index", or "/index.html" paths.
    // my_world_script:
    //     type: world
    //     data:
    //         index:
    //         - <!DOCTYPE html>
    //         - <html><head><title>Welcome to my site</title></head>
    //         - <body><h1>Hi!</h1></body></html>
    //     events:
    //         on webserver web request port:8080 path:/|/index|/index.html method:get:
    //         - determine code:200 passively
    //         - determine headers:[Content-Type=text/html] passively
    //         - determine raw_text_content:<script.data_key[data.index].separated_by[<n>]>
    //
    // @Example
    // # This example gives a default response to any pages not handled on port 8080.
    // on webserver web request port:8080 priority:1000 has_response:false:
    // - determine code:404 passively
    // - determine headers:[Content-Type=text/plain] passively
    // - determine "raw_text_content:Invalid path"
    //
    // @Example
    // # This example serves a favicon from the local file path "plugins/Denizen/webroot/favicon.ico" with RAM caching to any open web ports.
    // on webserver web request path:/favicon.ico:
    // - determine code:200 passively
    // - determine cached_file:favicon.ico
    //
    // -->

    public static WebserverWebRequestScriptEvent instance;

    public WebServerCommand.WebserverInstance server;
    public HttpExchange exchange;
    public WebResponse response;

    public static class WebResponse {

        public int code = 200;

        public String rawContent;

        public File fileResponse;

        public byte[] cachedFile;

        public boolean hasResponse = false;
    }

    public static HashMap<String, byte[]> responseFileCache = new HashMap<>();

    public WebserverWebRequestScriptEvent() {
        instance = this;
        registerCouldMatcher("webserver web request");
        registerSwitches("port", "path", "method", "has_response");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (path.switches.containsKey("method") && !runGenericSwitchCheck(path, "method", exchange.getRequestMethod())) {
            return false;
        }
        if (path.switches.containsKey("port") && !runGenericSwitchCheck(path, "port", String.valueOf(server.port))) {
            return false;
        }
        if (path.switches.containsKey("path") && !runGenericSwitchCheck(path, "path", exchange.getRequestURI().getPath())) {
            return false;
        }
        if (path.switches.containsKey("has_response") && !runGenericSwitchCheck(path, "has_response", String.valueOf(response.hasResponse))) {
            return false;
        }
        return super.matches(path);
    }

    public static byte[] readFileContent(File f) throws IOException {
        FileInputStream input = new FileInputStream(f);
        byte[] result = input.readAllBytes();
        input.close();
        return result;
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag) {
            String determination = determinationObj.toString();
            String determinationLow = CoreUtilities.toLowerCase(determination);
            if (determinationLow.startsWith("code:")) {
                String codeStr = determination.substring("code:".length());
                if (!new ElementTag(codeStr).isInt()) {
                    Debug.echoError("Invalid code '" + determination + "': not a number.");
                    return true;
                }
                response.code = new ElementTag(codeStr).asInt();
                return true;
            }
            else if (determinationLow.startsWith("headers:")) {
                TagContext context = getTagContext(path);
                MapTag map = MapTag.valueOf(determination.substring("headers:".length()), context);
                if (map == null) {
                    Debug.echoError("Invalid headers map input (not a MapTag)");
                    return true;
                }
                for (Map.Entry<StringHolder, ObjectTag> header : map.map.entrySet()) {
                    exchange.getResponseHeaders().set(header.getKey().str, header.getValue().toString());
                }
                return true;
            }
            else if (determinationLow.startsWith("raw_text_content:") && !response.hasResponse) {
                response.hasResponse = true;
                response.rawContent = determination.substring("raw_text_content:".length());
                return true;
            }
            else if ((determinationLow.startsWith("file:") || determinationLow.startsWith("cached_file:")) && !response.hasResponse) {
                response.hasResponse = true;
                File root = new File(DenizenCore.implementation.getDataFolder(), CoreConfiguration.webserverRoot);
                boolean isCaching = determinationLow.startsWith("cached_file:");
                String filePathName = determination.substring((isCaching ? "cached_file:" : "file:").length());
                if (isCaching) {
                    byte[] cached = responseFileCache.get(filePathName);
                    if (cached != null) {
                        response.cachedFile = cached;
                        return true;
                    }
                }
                File file = new File(root,  filePathName);
                if (!DenizenCore.implementation.canReadFile(file)) {
                    Debug.echoError("File path '" + determination + "' is not permitted for access by the Denizen config file.");
                    return true;
                }
                try {
                    if (!file.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                        Debug.echoError("File path '" + determination + "' is not within the web root.");
                        return true;
                    }
                }
                catch (IOException ex) {
                    Debug.echoError(ex);
                    return true;
                }
                if (isCaching) {
                    try {
                        response.cachedFile = readFileContent(file);
                    }
                    catch (IOException ex) {
                        Debug.echoError(ex);
                        return true;
                    }
                    responseFileCache.put(filePathName, response.cachedFile);
                }
                else {
                    response.fileResponse = file;
                }
                return true;
            }
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "method": return new ElementTag(exchange.getRequestMethod(), true);
            case "path": return new ElementTag(exchange.getRequestURI().getPath(), true);
            case "port": return new ElementTag(server.port);
            case "remote_address": return new ElementTag(exchange.getRemoteAddress().toString(), true);
            case "query": {
                MapTag output = new MapTag();
                String query = exchange.getRequestURI().getRawQuery();
                if (query != null) {
                    for (String pair : CoreUtilities.split(query, '&')) {
                        List<String> parts = CoreUtilities.split(pair, '=', 2);
                        try {
                            output.putObject(URLDecoder.decode(parts.get(0), "UTF-8"), new ElementTag(URLDecoder.decode(parts.get(1), "UTF-8"), true));
                        }
                        catch (UnsupportedEncodingException ex) {
                            Debug.echoError(ex);
                        }
                    }
                }
                return output;
            }
            case "raw_query": return exchange.getRequestURI().getRawQuery() == null ? null : new ElementTag(exchange.getRequestURI().getRawQuery(), true);
            case "raw_user_info": return exchange.getRequestURI().getRawUserInfo() == null ? null : new ElementTag(exchange.getRequestURI().getRawUserInfo(), null);
            case "headers": {
                MapTag output = new MapTag();
                for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                    ListTag list = new ListTag();
                    for (String str : header.getValue()) {
                        list.addObject(new ElementTag(str, true));
                    }
                    output.putObject(header.getKey(), list);
                }
                return output;
            }
            case "has_response": return new ElementTag(response.hasResponse);
            case "body": {
                try {
                    InputStream stream = exchange.getRequestBody();
                    if (stream == null) {
                        return null;
                    }
                    ElementTag result = new ElementTag(ScriptHelper.convertStreamToString(stream, true));
                    stream.close();
                    return result;
                }
                catch (IOException ex) {
                    Debug.echoError(ex);
                    return null;
                }
            }
        }
        return super.getContext(name);
    }

    @Override
    public String getName() {
        return "WebserverWebRequest";
    }

    public static void fire(WebServerCommand.WebserverInstance server, final HttpExchange exchange) {
        instance.server = server;
        instance.exchange = exchange;
        final WebResponse response = instance.response = new WebResponse();
        instance.fire();
        DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(() -> {
            try {
                byte[] body;
                if (response.rawContent != null) {
                    body = response.rawContent.getBytes(StandardCharsets.UTF_8);
                }
                else if (response.cachedFile != null) {
                    body = response.cachedFile;
                }
                else if (response.fileResponse != null) {
                    body = readFileContent(response.fileResponse);
                }
                else {
                    body = new byte[0];
                }
                exchange.sendResponseHeaders(response.code, body.length);
                OutputStream os = exchange.getResponseBody();
                os.write(body);
                os.close();
                exchange.close();
            }
            catch (Throwable ex) {
                if (!server.ignoreErrors || !(ex instanceof IOException)) {
                    DenizenCore.schedule(new OneTimeSchedulable(() -> {
                        Debug.echoError(ex);
                    }, 0));
                }
            }
        }, 0)));
    }
}
