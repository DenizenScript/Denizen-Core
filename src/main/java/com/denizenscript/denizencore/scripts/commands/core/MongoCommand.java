package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.*;
import org.bson.types.ObjectId;

import java.util.*;

public class MongoCommand extends AbstractCommand implements Holdable {
    public MongoCommand() {
        setName("mongo");
        setSyntax("mongo [id:<ID>] [connect:<uri> database:<database> collection:<collection>/disconnect/command:<command> parameter:<parameter>/command_map:<map>/find:<map> (by_id:<id>)/insert:<map>/update:<update> new:<new> (upsert:true/{false})/use_database:<database>/use_collection:<collection>");
        setRequiredArguments(2, 4);
        allowedDynamicPrefixes = true;
        isProcedural = false;
    }

    // <--[command]
    // @Name Mongo
    // @Short Interacts with a MongoDB server.
    //
    // @Description
    // This command is used to interact with a MongoDB server.
    //
    // MongoDB is a NoSQL database which uses concepts such as Documents and Collections to store data. MongoDB uses a form of JSON to represent its data.
    // It can interact with localhost connections as well as hosted connections (such as MongoDB's Atlas) via a connection URI.
    // Store your connection URI in the Denizen secrets file at 'plugins/Denizen/secrets.secret'. Refer to <@link ObjectType SecretTag> for usage info.
    //
    // Mongo works as a document-oriented database, where data is stored in Documents. Documents are stored inside Collections. Collections can contain many Documents. Collections are then stored inside Databases.
    //
    // Usage of the mongo command should almost always be used as ~awaitable (see <@link language ~waitable>), as large queries and insertions can take a while to retrieve or send.
    //
    // You can open a mongo connection with connect:<uri>. You must specify a database and collection to connect to with the database:<database> and collection:<collection> options.
    // You can change the database or collection you are connected to with use_database:<database> and use_collection:<collection>
    // If a Database or Collection you connect to does not exist, once you insert some data then the Database or Collection will be created automatically.
    //
    // To insert Documents, use insert:<map>.
    // To find a specific document from fragments of data, use find:<map>. You can include MongoDB's special query filters to further refine your query. If you want to search by a Document's ID, use by_id:id.
    //
    // To update a Document's data, use update:<update> with the old data, and new:<new> for the new data being updated.
    // You can also include the upsert flag, to create a new Document if the Document you are trying to update does not already exist.
    //
    // As MongoDB offers a variety of commands, to run a command not wrapped here you can use command:<command> and parameters:<parameters>. This will run a SINGLE basic command with parameters.
    // To run commands with multiple parameters, use command_map:<map>.
    //
    // TODO: When opening a connection, Mongo will output a lot of data to the console. There currently is not a way to turn this off.
    //
    // The mongo command is merely a wrapper, and further usage details should be gathered from an official MongoDB command reference rather than from Denizen command help.
    // You can view the official redis documentation and the supported commands here: <@link url https://www.mongodb.com/docs/manual/introduction/>
    //
    // @Tags
    // <util.mongo_connections> returns a ListTag of all the current Mongo connections.
    // <entry[saveName].result> returns the text result sent back from Mongo in a JSON format. JSON can be in an ElementTag or a ListTag depending on the action run.
    // <entry[saveName].inserted_id> returns the ID of the item that has been inserted via the `insert` action.
    // <entry[saveName].ok> returns the 'ok' value from the result. Used with the `command` and `command_map` actions.
    // <entry[saveName].upserted_id> returns the ID the upserted item. Returned if the `upsert` bool is true when updating.
    // <entry[saveName].updated_count> returns the amount of Documents updated via the `update` action.
    //
    // @Usage
    // Use to connect to a Mongo instance.
    // - ~mongo id:name connect:<secret[uri]>
    //
    // @Usage
    // Use to disconnect from a Mongo instance.
    // - mongo id:name disconnect
    //
    // @Usage
    // Run a simple command.
    // - ~mongo id:name command:dbStats parameter:1
    //
    // @Usage
    // Run more complex commands.
    // - definemap commands:
    //      count: my_collection
    //      skip: 4
    //  - ~mongo id:name command_map:<[commands]>
    //
    // @Usage
    // Simple find query.
    // - ~mongo id:name find:<map[name=Bob]>
    //
    // @Usage
    // Complex find query with query filters.
    // - definemap filters:
    //      $and:
    //          - number_greater_than:
    //              $gt: 2
    //          - number_less_than:
    //              $lt: 5
    // - ~mongo id:name find:<[filters]>
    //
    // @Usage
    // Insert data into a Collection.
    // - definemap data:
    //      name: Pluto
    //      order_from_sun: 9
    //      has_rings: false
    //      main_atmosphere:
    //          - N2
    //          - CH4
    //          - CO
    // - ~mongo id:name insert:<[data]> save:mg
    //
    // @Usage
    // Update data.
    // - definemap old_data:
    //      name: Pluto
    // - definemap new_data:
    //      $set:
    //          name: Pluto (A Dwarf Planet)
    // - ~mongo id:name update:<[old_data]> new:<[new_data]>
    //
    // @Usage
    // Change Databases.
    // - ~mongo id:name use_database:my_new_database
    //
    // @Usage
    // Change Collections.
    // - ~mongo id:name use_collection:my_new_collection
    // -->

    public static Map<String, MongoClient> connections = new HashMap<>();
    public static Map<String, MongoDatabase> databases = new HashMap<>();
    public static Map<String, MongoCollection<Document>> collections = new HashMap<>();

    @Override
    public void onDisable() {
        for (Map.Entry<String, MongoClient> entry : connections.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (final Exception e) {
                Debug.echoError(e);
            }
        }
        connections.clear();
        databases.clear();
        collections.clear();
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
            else if (!scriptEntry.hasObject("collection") && arg.matchesPrefix("collection", "col")) {
                scriptEntry.addObject("collection", arg.asElement());
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
            else if (!scriptEntry.hasObject("parameter")
                    && arg.matchesPrefix("parameter", "param", "p")) {
                scriptEntry.addObject("parameter", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("command_map", "cmd_map")) {
                scriptEntry.addObject("action", new ElementTag("command_map"));
                scriptEntry.addObject("command_map", arg.object);
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("find")) {
                scriptEntry.addObject("action", new ElementTag("find"));
                scriptEntry.addObject("filter", arg.object);
            }
            else if (!scriptEntry.hasObject("find_by_id")
                    && arg.matchesPrefix("by_id", "find_by_id")) {
                scriptEntry.addObject("find_by_id", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("insert", "i")) {
                scriptEntry.addObject("action", new ElementTag("insert"));
                scriptEntry.addObject("insert_data", arg.object);
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("update", "u")) {
                scriptEntry.addObject("action", new ElementTag("update"));
                scriptEntry.addObject("update", arg.object);
            }
            else if (!scriptEntry.hasObject("new")
                    && arg.matchesPrefix("new")) {
                scriptEntry.addObject("new", arg.object);
            }
            else if (!scriptEntry.hasObject("upsert")
                    && arg.matchesPrefix("upsert")
                    && arg.asElement().isBoolean()) {
                scriptEntry.addObject("upsert", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("use_database", "use_db")) {
                scriptEntry.addObject("action", new ElementTag("use_database"));
                scriptEntry.addObject("new_database", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("use_collection", "use_col")) {
                scriptEntry.addObject("action", new ElementTag("use_collection"));
                scriptEntry.addObject("new_collection", arg.asElement());
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
        if (!scriptEntry.hasObject("upsert")) {
            scriptEntry.defaultObject("upsert", new ElementTag("false"));
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag id = scriptEntry.getElement("id");
        ElementTag action = scriptEntry.getElement("action");
        ObjectTag uri = scriptEntry.getObjectTag("uri");
        ElementTag database = scriptEntry.getElement("database");
        ElementTag collection = scriptEntry.getElement("collection");
        ElementTag command = scriptEntry.getElement("command");
        ObjectTag parameter = scriptEntry.getObjectTag("parameter");
        MapTag commandMap = scriptEntry.getObjectTag("command_map");
        MapTag findFilter = scriptEntry.getObjectTag("filter");
        MapTag insertData = scriptEntry.getObjectTag("insert_data");
        MapTag updateData = scriptEntry.getObjectTag("update");
        MapTag newData = scriptEntry.getObjectTag("new");
        ElementTag upsert = scriptEntry.getElement("upsert");
        ElementTag findByID = scriptEntry.getElement("find_by_id");
        ElementTag useDatabase = scriptEntry.getElement("new_database");
        ElementTag useCollection = scriptEntry.getElement("new_collection");
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
                if (collection == null) {
                    Debug.echoError(scriptEntry, "You must specify a Collection!");
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
                    MongoDatabase db = null;
                    MongoCollection<Document> col = null;
                    try {
                        con = MongoClients.create(conStr);
                        db = con.getDatabase(database.asString());
                        col = db.getCollection(collection.asString());
                    } catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connection did not error.");
                    }
                    final MongoClient conn = con;
                    final MongoDatabase connDB = db;
                    final MongoCollection<Document> coll = col;
                    if (con != null) {
                        DenizenCore.runOnMainThread(() -> {
                            connections.put(id.asLowerString(), conn);
                            databases.put(id.asLowerString(), connDB);
                            collections.put(id.asLowerString(), coll);
                            Debug.echoDebug(scriptEntry, "Successfully connected to Mongo server.");
                            scriptEntry.setFinished(true);
                        });
                    }
                    else {
                        DenizenCore.runOnMainThread(() -> {
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoDebug(scriptEntry, "Connecting errored!");
                            }
                        });
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
                databases.remove(id.asLowerString());
                collections.remove(id.asLowerString());
                try {
                    con.close();
                } catch (Exception e) {
                    Debug.echoError(e);
                }
                Debug.echoDebug(scriptEntry, "Disconnected from '" + id.asString() + "'.");
            } else if (action.asString().equalsIgnoreCase("command")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (command == null) {
                    Debug.echoError("You must specify a command to run!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (parameter == null) {
                    Debug.echoError("You must specify the parameter to run!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        Object obj = parameter.getJavaObject();
                        Debug.echoDebug(scriptEntry, "Running command: " + command);
                        Document commandResult = db.runCommand(new Document(command.asString(), obj));
                        ElementTag okResult = new ElementTag(commandResult.get("ok").toString());
                        ElementTag resultRaw = new ElementTag(commandResult.toJson());
                        scriptEntry.addObject("result", resultRaw);
                        scriptEntry.addObject("ok", okResult);
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    } catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("command_map")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (commandMap == null) {
                    Debug.echoError("You must specify a map of commands and their data to run!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        HashMap<String, Object> commandsToIssue = new HashMap<>();
                        for (Map.Entry<StringHolder, ObjectTag> entry : commandMap.map.entrySet()) {
                            String key = entry.getKey().toString();
                            Object value = CoreUtilities.objectTagToJavaForm(entry.getValue(), false, true);
                            commandsToIssue.put(key, value);
                        }
                        Debug.echoDebug(scriptEntry, "Running commands: " + commandMap);
                        Document commandResult = db.runCommand(new Document(commandsToIssue));
                        ElementTag okResult = new ElementTag(commandResult.get("ok").toString());
                        ElementTag resultRaw = new ElementTag(commandResult.toJson());
                        scriptEntry.addObject("result", resultRaw);
                        scriptEntry.addObject("ok", okResult);
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    } catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("find")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                MongoCollection<Document> col = collections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (col == null) {
                    Debug.echoError(scriptEntry, "You need to specify a Collection to find Documents in!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (findFilter == null) {
                    Debug.echoError(scriptEntry, "You need to add filters to the find action!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        HashMap<String, Object> query = new HashMap<>();
                        for (Map.Entry<StringHolder, ObjectTag> entry : findFilter.map.entrySet()) {
                            String key = entry.getKey().toString();
                            Object value = CoreUtilities.objectTagToJavaForm(entry.getValue(), false, true);
                            query.put(key, value);
                        }
                        if (findByID != null) {
                            query.put("_id", new BsonObjectId(new ObjectId(findByID.toString())));
                        }
                        Debug.echoDebug(scriptEntry, "Finding data in Collection: '" + col.getNamespace() + "'. . .");
                        FindIterable<Document> findResult = col.find(new Document(query));
                        ListTag result = new ListTag();
                        for (Document doc : findResult) {
                            result.addObject(new ElementTag(doc.toJson()));
                        }
                        scriptEntry.addObject("result", result);
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("insert")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                MongoCollection<Document> col = collections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (col == null) {
                    Debug.echoError(scriptEntry, "You need to specify a Collection to find Documents in!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (insertData == null) {
                    Debug.echoError(scriptEntry, "You must specify data to insert!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        HashMap<String, Object> insert = new HashMap<>();
                        for (Map.Entry<StringHolder, ObjectTag> entry : insertData.map.entrySet()) {
                            String key = entry.getKey().toString();
                            Object value = CoreUtilities.objectTagToJavaForm(entry.getValue(), false, true);
                            insert.put(key, value);
                        }
                        Debug.echoDebug(scriptEntry, "Inserting data into Collection: '" + col.getNamespace() + "'. . .");
                        InsertOneResult result = col.insertOne(new Document(insert));
                        if (result.getInsertedId() != null) {
                            scriptEntry.addObject("inserted_id", new ElementTag(result.getInsertedId().asObjectId().getValue().toString()));
                        }
                        else {
                            scriptEntry.addObject("inserted_id", null);
                        }
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("update")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                MongoCollection<Document> col = collections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (col == null) {
                    Debug.echoError(scriptEntry, "You need to specify a Collection to find Documents in!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (updateData == null) {
                    Debug.echoError(scriptEntry, "You must specify the data you wish to update!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (newData == null) {
                    Debug.echoError(scriptEntry, "You must specify the new data to be updated!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        HashMap<String, Object> updateMap = new HashMap<>();
                        HashMap<String, Object> newMap = new HashMap<>();
                        for (Map.Entry<StringHolder, ObjectTag> entry : updateData.map.entrySet()) {
                            String key = entry.getKey().toString();
                            Object value = CoreUtilities.objectTagToJavaForm(entry.getValue(), false, true);
                            updateMap.put(key, value);
                        }
                        for (Map.Entry<StringHolder, ObjectTag> entry : newData.map.entrySet()) {
                            String key = entry.getKey().toString();
                            Object value = CoreUtilities.objectTagToJavaForm(entry.getValue(), false, true);
                            newMap.put(key, value);
                        }
                        Debug.echoDebug(scriptEntry, "Updating data. . .");
                        UpdateResult result;
                        if (upsert.asBoolean()) {
                            result = col.updateOne(new Document(updateMap), new Document(newMap), new UpdateOptions().upsert(true));
                            if (result.getUpsertedId() != null) {
                                scriptEntry.addObject("upserted_id", new ElementTag(result.getUpsertedId().asObjectId().getValue().toString()));
                            }
                            else {
                                scriptEntry.addObject("upserted_id", null);
                            }
                        } else {
                            result = col.updateOne(new Document(updateMap), new Document(newMap));
                        }
                        scriptEntry.addObject("updated_count", new ElementTag(result.getModifiedCount()));
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("use_database")) {
                MongoClient con = connections.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (useDatabase == null) {
                    Debug.echoError("You must provide a new Database to use!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        MongoDatabase newDB = con.getDatabase(useDatabase.asString());
                        Debug.echoDebug(scriptEntry, "Using new Database: '" + useDatabase + "'.");
                        databases.remove(id.asLowerString());
                        databases.put(id.asLowerString(), newDB);
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("use_collection")) {
                MongoClient con = connections.get(id.asLowerString());
                MongoDatabase db = databases.get(id.asLowerString());
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (db == null) {
                    Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (useCollection == null) {
                    Debug.echoError("You must provide a new Collection to use!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Runnable runnable = () -> {
                    try {
                        MongoCollection<Document> newCol = db.getCollection(useCollection.asString());
                        Debug.echoDebug(scriptEntry, "Using new Collection: '" + useCollection + "'.");
                        collections.remove(id.asLowerString());
                        collections.put(id.asLowerString(), newCol);
                        DenizenCore.runOnMainThread(() -> scriptEntry.setFinished(true));
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Mongo Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                };
                if (!scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
                }
                else {
                    runnable.run();
                }
            }
            else {
                Debug.echoError(scriptEntry, "Unknown action '" + action.asString() + "'!");
            }
        } catch (Exception e) {
            Debug.echoError("Mongo Exception: " + e.getMessage());
            if (CoreConfiguration.debugVerbose) {
                Debug.echoError(scriptEntry, e);
            }
        }
    }
}
