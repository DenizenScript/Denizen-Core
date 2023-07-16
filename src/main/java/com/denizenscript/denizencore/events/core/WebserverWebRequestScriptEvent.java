package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.BinaryTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.commands.core.WebServerCommand;
import com.denizenscript.denizencore.tags.ParseableTag;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
    // <context.body> returns the text content of the body that was sent, if any. Particularly for POST requests.
    // <context.body_binary> returns the raw binary content body that was sent, if any. Particularly for POST requests.
    // <context.has_response> returns true if a response body determination (raw_text_content, file, or cached_file) was applied, or false if not.
    //
    // @Determine
    // "CODE:<Element(Number)>" to set a standard web response code, such as 'code:200' for 'OK', or 'code:404' for 'File Not Found'
    // "HEADERS": + MapTag to set a map of headers, where map keys are the header name and map values are the text of the value, for example headers:[Content-Type=text/html] ... note that header are sometimes case-sensitive.
    // "RAW_TEXT_CONTENT:<ElementTag>" to set a raw text content body in response. You may determine only one response - raw text, raw binary, a file, or a cached file. You cannot use multiple.
    // "RAW_BINARY_CONTENT:<BinaryTag>" to set a raw binary content body in response.
    // "FILE:<ElementTag>" to set a path to a file to send in response. File path must be within the web-root path configured in Denizen/config.yml. Files will be read async.
    // "CACHED_FILE:<ElementTag>" to set a path to a file to send in response. The content of the file will be cached in RAM until the server restarts. This is useful for files that definitely won't change. First file read will be sync, all others are instant.
    // "PARSED_FILE:<ElementTag>" - like "FILE:", but this file will be parsed for tags using syntax like "<{util.pi}>" to separate tags from HTML entries.
    // "CACHED_PARSED_FILE:<ElementTag>" - like "PARSED_FILE" and "CACHED_FILE" combined. Note that the file will be cached, but the results of tags will be handled at runtime still.
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

    public byte[] getBody() {
        if (response.inputBody != null) {
            return response.inputBody;
        }
        try {
            InputStream stream = exchange.getRequestBody();
            if (stream == null) {
                return null;
            }
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = stream.read(buffer, 0, 1024)) != -1) {
                bytesOut.write(buffer, 0, len);
            }
            response.inputBody = bytesOut.toByteArray();
            bytesOut.close();
            return response.inputBody;
        }
        catch (IOException ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static class WebResponse {

        public int code = 200;

        public byte[] rawContent;

        public File fileResponse;

        public byte[] cachedFile;

        public boolean hasResponse = false;

        public byte[] inputBody;
    }

    public static HashMap<String, byte[]> responseFileCache = new HashMap<>();

    public static HashMap<String, ParseableTag> responseParseableCache = new HashMap<>();

    @Override
    public void destroy() {
        responseFileCache.clear();
        responseParseableCache.clear();
    }

    public WebserverWebRequestScriptEvent() {
        instance = this;
        registerCouldMatcher("webserver web request");
        registerSwitches("port", "path", "method", "has_response");
        this.<WebserverWebRequestScriptEvent, ElementTag>registerOptionalDetermination("code", ElementTag.class, (evt, context, code) -> {
            if (!code.isInt()) {
                Debug.echoError("Invalid code '" + code + "': not an number");
                return false;
            }
            evt.response.code = code.asInt();
            return true;
        });
        this.<WebserverWebRequestScriptEvent, MapTag>registerDetermination("headers", MapTag.class, (evt, context, headers) -> {
            for (Map.Entry<StringHolder, ObjectTag> header : headers.entrySet()) {
                evt.exchange.getResponseHeaders().set(header.getKey().str, header.getValue().toString());
            }
        });
        registerResponseDetermination("raw_text_content", ElementTag.class, (evt, context, rawText) -> {
            evt.response.rawContent = rawText.asString().getBytes(StandardCharsets.UTF_8);
            return true;
        });
        registerResponseDetermination("raw_binary_content", BinaryTag.class, (evt, context, rawBinary) -> {
            evt.response.rawContent = rawBinary.data;
            return true;
        });
        registerResponseDetermination("file", ElementTag.class, (evt, context, file) -> evt.handleFileDetermination(false, false, file.asString(), context));
        registerResponseDetermination("parsed_file", ElementTag.class, (evt, context, file) -> evt.handleFileDetermination(false, true, file.asString(), context));
        registerResponseDetermination("cached_file", ElementTag.class, (evt, context, file) -> evt.handleFileDetermination(true, false, file.asString(), context));
        registerResponseDetermination("cached_parsed_file", ElementTag.class, (evt, context, file) -> evt.handleFileDetermination(true, true, file.asString(), context));
        registerResponseDetermination("parsed_cached_file", ElementTag.class, (evt, context, file) -> evt.handleFileDetermination(true, true, file.asString(), context));
    }

    public <T extends ObjectTag> void registerResponseDetermination(String prefix, Class<T> inputType, OptionalDeterminationHandler<WebserverWebRequestScriptEvent, T> handler) {
        this.<WebserverWebRequestScriptEvent, T>registerOptionalDetermination(prefix, inputType, (event, context, determination) -> {
            if (!event.response.hasResponse) {
                event.response.hasResponse = true;
                return handler.handle(event, context, determination);
            }
            return false;
        });
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

    public boolean handleFileDetermination(boolean cache, boolean parse, String determination, TagContext context) {
        response.hasResponse = true;
        File root = new File(DenizenCore.implementation.getDataFolder(), CoreConfiguration.webserverRoot);
        if (cache) {
            if (parse) {
                ParseableTag tag = responseParseableCache.get(determination);
                if (tag != null) {
                    response.cachedFile = tag.parse(context).identify().getBytes(StandardCharsets.UTF_8);
                    return true;
                }
            }
            byte[] cached = responseFileCache.get(determination);
            response.cachedFile = cached;
            if (cached != null) {
                if (parse) {
                    ParseableTag tag = TagManager.parseTextToTagInternal(new String(response.cachedFile, StandardCharsets.UTF_8), context, true);
                    responseParseableCache.put(determination, tag);
                    response.cachedFile = tag.parse(context).identify().getBytes(StandardCharsets.UTF_8);
                }
                return true;
            }
        }
        File file = new File(root, determination);
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
        if (cache || parse) {
            try {
                response.cachedFile = readFileContent(file);
            }
            catch (IOException ex) {
                Debug.echoError(ex);
                return true;
            }
            if (cache) {
                responseFileCache.put(determination, response.cachedFile);
            }
            if (parse) {
                ParseableTag tag = TagManager.parseTextToTagInternal(new String(response.cachedFile, StandardCharsets.UTF_8), context, true);
                if (cache) {
                    responseParseableCache.put(determination, tag);
                }
                response.cachedFile = tag.parse(context).identify().getBytes(StandardCharsets.UTF_8);
            }
        }
        else {
            response.fileResponse = file;
        }
        return true;
    }

    @Override
    public ObjectTag getContext(String name) {
        return switch (name) {
            case "method" -> new ElementTag(exchange.getRequestMethod(), true);
            case "path" -> new ElementTag(exchange.getRequestURI().getPath(), true);
            case "port" -> new ElementTag(server.port);
            case "remote_address" -> new ElementTag(exchange.getRemoteAddress().toString(), true);
            case "query" -> {
                MapTag output = new MapTag();
                String query = exchange.getRequestURI().getRawQuery();
                if (query != null) {
                    for (String pair : CoreUtilities.split(query, '&')) {
                        List<String> parts = CoreUtilities.split(pair, '=', 2);
                        output.putObject(URLDecoder.decode(parts.get(0), StandardCharsets.UTF_8), new ElementTag(URLDecoder.decode(parts.get(1), StandardCharsets.UTF_8), true));
                    }
                }
                yield output;
            }
            case "raw_query" -> exchange.getRequestURI().getRawQuery() == null ? null : new ElementTag(exchange.getRequestURI().getRawQuery(), true);
            case "raw_user_info" -> exchange.getRequestURI().getRawUserInfo() == null ? null : new ElementTag(exchange.getRequestURI().getRawUserInfo(), null);
            case "headers" -> {
                MapTag output = new MapTag();
                for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                    output.putObject(header.getKey(), new ListTag(header.getValue(), true));
                }
                yield output;
            }
            case "has_response" -> new ElementTag(response.hasResponse);
            case "body" -> new ElementTag(new String(getBody(), StandardCharsets.UTF_8));
            case "body_binary" -> new BinaryTag(getBody());
            default -> super.getContext(name);
        };
    }

    public static void fire(WebServerCommand.WebserverInstance server, final HttpExchange exchange) {
        instance.server = server;
        instance.exchange = exchange;
        final WebResponse response = instance.response = new WebResponse();
        instance.fire();
        DenizenCore.runAsync(() -> {
            try {
                byte[] body;
                if (response.rawContent != null) {
                    body = response.rawContent;
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
                    Debug.echoError(ex);
                }
            }
        });
    }
}
