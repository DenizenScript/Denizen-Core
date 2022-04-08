package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.WebserverWebRequestScriptEvent;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
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
        setPrefixesHandled("port");
        setBooleansHandled("ignore_errors");
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

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("mode")
               && arg.matchesEnum(Mode.class)) {
                scriptEntry.addObject("mode", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("mode")) {
            throw new InvalidArgumentsException("Missing start/stop argument.");
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        if (!CoreConfiguration.allowWebserver) {
            Debug.echoError(scriptEntry, "WebServer command disabled in config.yml!");
            return;
        }
        ElementTag port = scriptEntry.argForPrefixAsElement("port", "8080");
        ElementTag mode = scriptEntry.getElement("mode");
        boolean ignoreErrors = scriptEntry.argAsBoolean("ignore_errors");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), mode, port, db("ignore_errors", ignoreErrors));
        }
        if (!port.isInt()) {
            throw new InvalidArgumentsRuntimeException("Invalid port, not a number");
        }
        int portNum = port.asInt();
        switch (mode.asEnum(Mode.class)) {
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
