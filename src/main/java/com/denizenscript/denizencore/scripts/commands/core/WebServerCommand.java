package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.WebserverWebRequestScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class WebServerCommand extends AbstractCommand {

    public WebServerCommand() {
        setName("webserver");
        setSyntax("webserver [start/stop] (port:<#>) (ignore_errors)");
        setRequiredArguments(1, 3);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name WebServer
    // @Syntax webserver [start/stop] (port:<#>) (ignore_errors)
    // @Required 1
    // @Maximum 3
    // @Short Creates a local HTTP web-server within your minecraft server.
    // @Group core
    //
    // @Description
    // Creates a local HTTP web-server within your minecraft server.
    //
    // The server does not provide SSL (HTTPS) security or functionality.
    // The server does not provide active abuse-prevention or routing control or etc.
    //
    // If your webserver is meant for public connection, it is very strongly recommended you put the webserver behind a reverse-proxy server, such as Nginx or Apache2.
    //
    // The port, if unspecified, defaults to 8080. You should usually manually specify a port.
    //
    // The "ignore_errors" option can be enabled to silence basic connection errors that might otherwise spam your console logs.
    //
    // You can only exactly one webserver per port.
    // If you use multiple ports, you can thus have multiple webservers.
    //
    // When using the stop instruction, you must specify the same port you used when starting.
    //
    // The webserver only does anything if you properly handle <@link event webserver web request>
    //
    // Most webserver processing is done in the event, and thus is synchronous with the minecraft thread, and thus may induce lag if not done with care.
    // Note per the event's meta, "file:" is handled async, and "cached_file:" only runs sync once per file.
    //
    // This command must be enabled in the Denizen/config.yml before it can be used.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to start a webserver on port 8081.
    // - webserver start port:8081
    //
    // @Usage
    // Use to stop the webserver on port 8081.
    // - webserver stop port:8081
    //
    // -->

    public static class WebserverInstance {

        public int port;

        public HttpServer server;

        public boolean ignoreErrors;

        public void handleRequest(HttpExchange exchange) {
            WebserverWebRequestScriptEvent.fire(this, exchange);
        }

        public void executor(Runnable command) {
            DenizenCore.schedule(new OneTimeSchedulable(command, 0));
        }

        public void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this::handleRequest);
            server.setExecutor(this::executor);
            server.start();
        }

        public void stop() {
            server.stop(0);
        }
    }

    public static HashMap<Integer, WebserverInstance> webservers = new HashMap<>();

    public enum Mode { START, STOP }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("port") @ArgDefaultText("8080") int portNum,
                                   @ArgName("mode") Mode mode,
                                   @ArgName("ignore_errors") boolean ignoreErrors) {
        if (!CoreConfiguration.allowWebserver) {
            Debug.echoError("WebServer command disabled in config.yml!");
            return;
        }
        switch (mode) {
            case START: {
                WebserverInstance instance = webservers.get(portNum);
                if (instance != null) {
                    Debug.echoError("Server already running at port " + portNum + ", cannot start a new one.");
                    return;
                }
                instance = new WebserverInstance();
                instance.port = portNum;
                instance.ignoreErrors = ignoreErrors;
                try {
                    instance.start();
                    webservers.put(portNum, instance);
                    Debug.echoDebug(scriptEntry, "Webserver at port " + portNum + " started.");
                }
                catch (IOException ex) {
                    Debug.echoError("Could not start webserver due to IOException. Is the port correct?");
                    Debug.echoError(ex);
                }
                break;
            }
            case STOP: {
                WebserverInstance instance = webservers.remove(portNum);
                if (instance == null) {
                    Debug.echoDebug(scriptEntry, "No server running at port " + portNum + ", ignoring 'stop' instruction.");
                    return;
                }
                instance.stop();
                Debug.echoDebug(scriptEntry, "Webserver at port " + portNum + " stopped.");
                break;
            }
        }
    }
}
