package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
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
        setSyntax("mongo [id:<ID>] [connect:<uri> database:<database> collection:<collection>/disconnect/command:<map>/find:<map> (by_id:<id>)/insert:<map>/update:<update> new:<new> (upsert:true/{false})/use_database:<database>/use_collection:<collection>]");
        setRequiredArguments(2, 4);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name Mongo
    // @Syntax mongo [id:<ID>] [connect:<uri> database:<database> collection:<collection>/disconnect/command:<map>/find:<map> (by_id:<id>)/insert:<map>/update:<update> new:<new> (upsert:true/{false})/use_database:<database>/use_collection:<collection>]
    // @Required 2
    // @Maximum 4
    // @Short Interacts with a MongoDB server.
    // @Group core
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
    // Usage of the mongo command should almost always be used as ~waitable (see <@link language ~waitable>), as large queries and insertions can take a while to retrieve or send.
    //
    // You can open a mongo connection with connect:<uri>. You must specify a database and collection to connect to with the database:<database> and collection:<collection> options.
    // You can change the database or collection you are connected to with use_database:<database> and use_collection:<collection>
    // If a Database or Collection you connect to does not exist, once you insert some data then the Database or Collection will be created automatically.
    //
    // To insert Documents, use insert:<map>.
    // To find a specific document from fragments of data, use find:<map>. You can include MongoDB's special query filters to further refine your query. If you want to search by a Document's ID, use by_id:id.
    //
    // To update a Document's data, use update:<update> with the old data, and new:<new> for the new data being updated. This will update every Document matched with the provided data.
    // You can also include the upsert flag, to create a new Document if the Document you are trying to update does not already exist.
    //
    // As MongoDB offers a variety of commands, to run a command not wrapped here you can use command:<map>.
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
    // <entry[saveName].ok> returns the 'ok' value from the result. Used with the `command` action.
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
    // - ~mongo id:name command:<map[dbStats=1]>
    //
    // @Usage
    // Run more complex commands.
    // - definemap commands:
    //      count: my_collection
    //      skip: 4
    //  - ~mongo id:name command:<[commands]>
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

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("id") String id,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("connect") SecretTag uri,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("database") String database,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("collection") String collection,
                                   // This is a boolean because it will let me check if it is present without using a prefix.
                                   @ArgName("disconnect") boolean disconnect,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("command") MapTag command,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("find") MapTag findQuery,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("by_id") String findByID,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("insert") MapTag insert,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("update") MapTag oldData,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("new") MapTag newData,
                                   @ArgName("upsert") boolean upsert,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("use_database") String newDatabase,
                                   @ArgPrefixed @ArgDefaultNull @ArgName("use_collection") String newCollection) {
        if (!CoreConfiguration.allowMongo) {
            Debug.echoError(scriptEntry, "Mongo disabled by config!");
            return;
        }
        if (id == null) {
            Debug.echoError(scriptEntry, "Missing connection ID!");
            return;
        }
        String connectionId = CoreUtilities.toLowerCase(id);
        Runnable runnable = () -> {
            try {
                if (uri != null) {
                    if (connections.containsKey(connectionId)) {
                        Debug.echoError(scriptEntry, "Already connected to a server with ID '" + id + "'!");
                        scriptEntry.setFinished(true);
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
                        String conStr = uri.getValue();
                        if (!conStr.startsWith("mongodb://") && !conStr.startsWith("mongodb+srv://")) {
                            conStr = "mongodb://" + conStr;
                        }
                        if (CoreConfiguration.debugVerbose) {
                            Debug.echoDebug(scriptEntry, "Connecting to Mongo server...");
                        }
                        MongoDatabase db = null;
                        MongoCollection<Document> col = null;
                        try {
                            con = MongoClients.create(conStr);
                            db = con.getDatabase(database);
                            col = db.getCollection(collection);
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
                        if (CoreConfiguration.debugVerbose) {
                            Debug.echoDebug(scriptEntry, "Connection did not error.");
                        }
                        final MongoClient conn = con;
                        final MongoDatabase connDB = db;
                        final MongoCollection<Document> coll = col;
                        if (con != null) {
                            DenizenCore.runOnMainThread(() -> {
                                connections.put(connectionId, conn);
                                databases.put(connectionId, connDB);
                                collections.put(connectionId, coll);
                                Debug.echoDebug(scriptEntry, "Successfully connected to Mongo server.");
                                scriptEntry.setFinished(true);
                            });
                        }
                        else {
                            DenizenCore.runOnMainThread(() -> {
                                Debug.echoDebug(scriptEntry, "Connecting errored!");
                                scriptEntry.setFinished(true);
                            });
                        }
                    }, 0)));
                }
                // If disconnect is true, it means it is present.
                else if (disconnect) {
                    MongoClient con = connections.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    connections.remove(connectionId);
                    databases.remove(connectionId);
                    collections.remove(connectionId);
                    try {
                        con.close();
                    }
                    catch (Exception e) {
                        Debug.echoError(e);
                    }
                    Debug.echoDebug(scriptEntry, "Disconnected from '" + id + "'.");
                }
                else if (command != null) {
                    MongoClient con = connections.get(connectionId);
                    MongoDatabase db = databases.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    if (db == null) {
                        Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    try {
                        Debug.echoDebug(scriptEntry, "Running commands: " + command);
                        HashMap<String, Object> finalCommand = (HashMap<String, Object>) CoreUtilities.objectTagToJavaForm(command, false, true);
                        Document commandResult = db.runCommand(new Document(finalCommand));
                        ElementTag okResult = new ElementTag(commandResult.get("ok").toString());
                        ElementTag resultRaw = new ElementTag(commandResult.toJson());
                        scriptEntry.addObject("result", resultRaw);
                        scriptEntry.addObject("ok", okResult);
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
                }
                else if (findQuery != null) {
                    MongoClient con = connections.get(connectionId);
                    MongoDatabase db = databases.get(connectionId);
                    MongoCollection<Document> col = collections.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
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
                    try {
                        HashMap<String, Object> query = (HashMap<String, Object>) CoreUtilities.objectTagToJavaForm(findQuery, false, true);
                        if (findByID != null) {
                            query.put("_id", new BsonObjectId(new ObjectId(findByID)));
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
                }
                else if (insert != null) {
                    MongoClient con = connections.get(connectionId);
                    MongoDatabase db = databases.get(connectionId);
                    MongoCollection<Document> col = collections.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
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
                    try {
                        HashMap<String, Object> insertData = (HashMap<String, Object>) CoreUtilities.objectTagToJavaForm(insert, false, true);
                        Debug.echoDebug(scriptEntry, "Inserting data into Collection: '" + col.getNamespace() + "'. . .");
                        InsertOneResult result = col.insertOne(new Document(insertData));
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
                }
                else if (oldData != null) {
                    MongoClient con = connections.get(connectionId);
                    MongoDatabase db = databases.get(connectionId);
                    MongoCollection<Document> col = collections.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
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
                    if (newData == null) {
                        Debug.echoError(scriptEntry, "You must specify the new data to be updated!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    try {
                        HashMap<String, Object> updateMap = (HashMap<String, Object>) CoreUtilities.objectTagToJavaForm(oldData, false, true);
                        HashMap<String, Object> newMap = (HashMap<String, Object>) CoreUtilities.objectTagToJavaForm(newData, false, true);
                        Debug.echoDebug(scriptEntry, "Updating data. . .");
                        UpdateResult result;
                        if (upsert) {
                            result = col.updateMany(new Document(updateMap), new Document(newMap), new UpdateOptions().upsert(true));
                            if (result.getUpsertedId() != null) {
                                scriptEntry.addObject("upserted_id", new ElementTag(result.getUpsertedId().asObjectId().getValue().toString()));
                            }
                            else {
                                scriptEntry.addObject("upserted_id", null);
                            }
                        }
                        else {
                            result = col.updateMany(new Document(updateMap), new Document(newMap));
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
                }
                else if (newDatabase != null) {
                    MongoClient con = connections.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    try {
                        MongoDatabase newDB = con.getDatabase(newDatabase);
                        Debug.echoDebug(scriptEntry, "Using new Database: '" + newDatabase + "'.");
                        databases.remove(id.toLowerCase());
                        databases.put(id.toLowerCase(), newDB);
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
                }
                else if (newCollection != null) {
                    MongoClient con = connections.get(connectionId);
                    MongoDatabase db = databases.get(connectionId);
                    if (con == null) {
                        Debug.echoError(scriptEntry, "Not connected to server with ID: '" + id + "'!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    if (db == null) {
                        Debug.echoError(scriptEntry, "Not connected to database! Was it dropped?");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    try {
                        MongoCollection<Document> newCol = db.getCollection(newCollection);
                        Debug.echoDebug(scriptEntry, "Using new Collection: '" + newCollection + "'.");
                        collections.remove(id.toLowerCase());
                        collections.put(id.toLowerCase(), newCol);
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
                }
                else {
                    Debug.echoError(scriptEntry, "Invalid mongo action!");
                    scriptEntry.setFinished(true);
                }
            }
            catch (Exception e) {
                Debug.echoError("Mongo Exception: " + e.getMessage());
                if (CoreConfiguration.debugVerbose) {
                    Debug.echoError(scriptEntry, e);
                }
            }
        };
        if (!scriptEntry.shouldWaitFor()) {
            DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runnable, 0)));
        }
        else {
            runnable.run();
        }
    }
}
