package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.*;

import java.util.HashMap;
import java.util.Map;

public class MongoCommand extends AbstractCommand implements Holdable {
    public MongoCommand() {
        setName("mongo");
        setSyntax("mongo [id:<ID>] [connect:<uri> database:<database>/disconnect/command:<command> data:<data>]");
        setRequiredArguments(2, 100);
        allowedDynamicPrefixes = true;
        isProcedural = false;
    }

    // <--[command]
    // @Name Mongo
    // @Short Interacts with a MongoDB server.
    //
    // @Tags
    // <util.mongo_connections> returns a ListTag of all the current Mongo connections.
    // <entry[saveName].result> returns the text result sent back from Mongo in a JSON format.
    // <entry[saveName].ok_result> returns the 'ok' value from the result.
    // -->

    // TODO: Mongo will log a whole bunch of stuff to the console. Add way to prevent this.
    public static Map<String, MongoClient> connections = new HashMap<>();
    public static Map<String, MongoDatabase> databases = new HashMap<>();

    @Override
    public void onDisable() {
        for (Map.Entry<String, MongoClient> entry : connections.entrySet()) {
            try {
                entry.getValue().close();
            } catch (final Exception e) {
                Debug.echoError(e);
            }
        }
        connections.clear();
    }

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("id:", connections.keySet());
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("id")
                    && arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("connect")) {
                scriptEntry.addObject("action", new ElementTag("connect"));
                scriptEntry.addObject("uri", arg.object);
            }
            else if (!scriptEntry.hasObject("database")
                    && arg.matchesPrefix("database", "db")) {
                scriptEntry.addObject("database", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matches("disconnect")) {
                scriptEntry.addObject("action", new ElementTag("disconnect"));
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("command", "cmd", "c")) {
                scriptEntry.addObject("action", new ElementTag("command"));
                scriptEntry.addObject("command", arg.asElement());
            }
            else if (!scriptEntry.hasObject("data")
                    && arg.matchesPrefix("data", "d")) {
                scriptEntry.addObject("data", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id")) {
            throw new InvalidArgumentsException("Must specify an ID!");
        }
        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify an action!");
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag id = scriptEntry.getElement("id");
        ElementTag action = scriptEntry.getElement("action");
        ObjectTag uri = scriptEntry.getObjectTag("uri");
        ElementTag database = scriptEntry.getElement("database");
        ElementTag command = scriptEntry.getElement("command");
        ObjectTag data = scriptEntry.getObjectTag("data");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), id, uri, database, action);
        }
        try {
            if (action.asString().equalsIgnoreCase("connect")) {
                if (connections.containsKey(id.asString().toLowerCase())) {
                    Debug.echoError(scriptEntry, "Already connected to a server with ID '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                String rawUri;
                if (uri.shouldBeType(SecretTag.class)) {
                    SecretTag secret = uri.asType(SecretTag.class, scriptEntry.context);
                    if (secret == null) {
                        Debug.echoError("Invalid URI SecretTag object '" + uri.asElement().asString() + "' - secret not defined in 'secrets.secret'?");
                        return;
                    }
                    rawUri = secret.getValue();
                } else {
                    Debug.echoError("Connection URI must be of type SecretTag!");
                    return;
                }
                if (database == null) {
                    Debug.echoError(scriptEntry, "Must specify a database!");
                    scriptEntry.setFinished(true);
                    return;
                }
                DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(() -> {
                    MongoClient con = null;
                    String conStr = rawUri;
                    if (!conStr.startsWith("mongodb://") && !conStr.startsWith("mongodb+srv://")) {
                        conStr = "mongodb://" + conStr;
                    }
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connecting to Mongo server. . .");
                    }
                    try {
                        con = MongoClients.create(conStr);
                    } catch (final Exception e) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        }, 0));
                    }
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connection did not error.");
                    }
                    MongoDatabase db = null;
                    try {
                        db = con.getDatabase(database.asString());
                    } catch (final Exception e) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        }, 0));
                    }
                    final MongoClient conn = con;
                    final MongoDatabase connDB = db;
                    if (con != null) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            connections.put(id.asString().toLowerCase(), conn);
                            databases.put(id.asString().toLowerCase(), connDB);
                            Debug.echoDebug(scriptEntry, "Successfully connected to Mongo server.");
                            scriptEntry.setFinished(true);
                        }, 0));
                    } else {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoDebug(scriptEntry, "Connecting errored!");
                            }
                        }, 0));
                    }
                }, 0)));
            } else if (action.asString().equalsIgnoreCase("disconnect")) {
                MongoClient con = connections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                connections.remove(id.asLowerString());
                try {
                    con.close();
                } catch (Exception e) {
                    Debug.echoError(e);
                }
                Debug.echoDebug(scriptEntry, "Disconnected from '" + id.asString() + "'.");
            } else if (action.asString().equalsIgnoreCase("command")) {
                MongoClient con = connections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (command == null) {
                    Debug.echoError("You must specify a command to run!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (data == null) {
                    Debug.echoError("You must specify the data to run!");
                    scriptEntry.setFinished(true);
                    return;
                }
                MongoDatabase db = databases.get(id.asLowerString());
                Runnable runnable = () -> {
                    try {
                        Object obj = data.getJavaObject();
                        // TODO: See what would happen if a MapTag was passed in. Will it create multiple command arguments? Or will it error? ...
                        // TODO: ... So far I have only tested with a single ElementTag.
                        Document commandResult = db.runCommand(new Document(command.asString(), obj));
                        ElementTag okResult = new ElementTag(commandResult.get("ok").toString());
                        ElementTag resultRaw = new ElementTag(commandResult.toJson());
                        scriptEntry.addObject("result", resultRaw);
                        scriptEntry.addObject("ok_result", okResult);
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            scriptEntry.setFinished(true);
                        }, 0));
                    } catch (final Exception e) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        }, 0));
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
        } catch (Exception e) {
            Debug.echoError("Mongo Exception: " + e.getMessage());
            if (CoreConfiguration.debugVerbose) {
                Debug.echoError(scriptEntry, e);
            }
        }
    }
}