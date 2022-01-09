package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SQLCommand extends AbstractCommand implements Holdable {

    public SQLCommand() {
        setName("sql");
        setSyntax("sql [id:<ID>] [disconnect/connect:<server> (username:<username>) (passwordfile:<file>) (ssl:true/{false})/query:<query>/update:<update>]");
        setRequiredArguments(2, 5);
        isProcedural = false;
    }

    // <--[command]
    // @Name SQL
    // @Syntax sql [id:<ID>] [disconnect/connect:<server> (username:<username>) (passwordfile:<file>) (ssl:true/{false})/query:<query>/update:<update>]
    // @Required 2
    // @Maximum 5
    // @Short Interacts with a MySQL server.
    // @Group core
    //
    // @Description
    // This command is used to interact with a MySQL server. It can update the database or query it for information.
    //
    // This commands exists primarily for interoperability with pre-existing databases and external services.
    // It should never be used for storing data that only Denizen needs to use. Consider instead using <@link command flag>.
    //
    // The general usage order is connect -> update/query -> disconnect.
    // It is not required that you disconnect right after using, and in fact encouraged that you keep a connection open where possible.
    //
    // When connecting, the server format is IP:Port/Database, EG 'localhost:3306/test'.
    // You can also append options to the end, like 'localhost:3306/test?autoReconnect=true'
    // You can create a simple text file to contain the password, and store it in 'plugins/Denizen' or a subfolder, and input it using the 'passwordfile' option.
    //
    // You can switch whether SSL is used for the connection (defaults to false).
    //
    // Note that when using tag, it is recommended you escape unusual inputs to avoid SQL injection.
    //
    // The SQL command is merely a wrapper for SQL queries, and further usage details should be gathered from an official MySQL query reference rather than from Denizen command help.
    //
    // SQL connections are not instant - they can take several seconds, or just never connect at all.
    // It is recommended you hold the command by doing "- ~sql ..." rather than just "- sql ..."
    // as this will delay the commands following the SQL command until after the SQL operation is complete.
    //
    // If you have an SQL database server other than MySQL, be sure to include the driver prefix (defaults to "mysql://" when unspecified).
    //
    // @Tags
    // <entry[saveName].result_list> returns a ListTag of all row ListTags from a query or update command. That's a ListTag of ListTags, so for example to get the second entry of the first row you'd use "result_list.get[1].get[2]"
    // <entry[saveName].affected_rows> returns how many rows were affected by an update command.
    //
    // @Usage
    // Use to connect to an SQL server.
    // - ~sql id:name connect:localhost:3306/test username:space passwordfile:pw.txt
    //
    // @Usage
    // Use to connect to an SQL server over an SSL connection.
    // - ~sql id:name connect:localhost:3306/test username:space passwordfile:pw.txt ssl:true
    //
    // @Usage
    // Use to connect to an SQL server with a UTF8 text encoding.
    // - ~sql id:name connect:localhost:3306/test?characterEncoding=utf8 username:space passwordfile:pw.txt
    //
    // @Usage
    // Use to update an SQL server.
    // - ~sql id:name "update:CREATE table things(id int,column_name1 varchar(255),column_name2 varchar(255));"
    //
    // @Usage
    // Use to update an SQL server.
    // - ~sql id:name "update:INSERT INTO things VALUES (3, 'hello', 'space');"
    //
    // @Usage
    // Use to query an SQL server.
    // - ~sql id:name "query:SELECT id,column_name1,column_name2 FROM things;" save:saveName
    // - narrate <entry[saveName].result>
    //
    // @Usage
    // Use to query an SQL server.
    // - ~sql id:name "query:SELECT id,column_name1,column_name2 FROM things WHERE id=3;" save:saveName2
    // - narrate <entry[saveName2].result>
    //
    // @Usage
    // Use to disconnect from an SQL server.
    // - sql disconnect id:name
    // -->

    public static Map<String, Connection> connections = new HashMap<>();

    @Override
    public void onDisable() {
        for (Map.Entry<String, Connection> entry : connections.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (SQLException e) {
                Debug.echoError(e);
            }
        }
        connections.clear();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("sqlid")
                    && arg.matchesPrefix("id")) {
                scriptEntry.addObject("sqlid", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("connect")) {
                scriptEntry.addObject("action", new ElementTag("CONNECT"));
                scriptEntry.addObject("server", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matches("disconnect")) {
                scriptEntry.addObject("action", new ElementTag("DISCONNECT"));
            }
            else if (!scriptEntry.hasObject("query")
                    && arg.matchesPrefix("query")) {
                scriptEntry.addObject("action", new ElementTag("QUERY"));
                scriptEntry.addObject("query", arg.asElement());
            }
            else if (!scriptEntry.hasObject("query")
                    && arg.matchesPrefix("update")) {
                scriptEntry.addObject("action", new ElementTag("UPDATE"));
                scriptEntry.addObject("query", arg.asElement());
            }
            else if (!scriptEntry.hasObject("username")
                    && arg.matchesPrefix("username")) {
                scriptEntry.addObject("username", arg.asElement());
            }
            else if (!scriptEntry.hasObject("password")
                    && arg.matchesPrefix("password")) {
                scriptEntry.addObject("password", arg.asElement());
            }
            else if (!scriptEntry.hasObject("passwordfile")
                    && arg.matchesPrefix("passwordfile")) {
                scriptEntry.addObject("passwordfile", arg.asElement());
            }
            else if (!scriptEntry.hasObject("ssl")
                    && arg.matchesPrefix("ssl")
                    && arg.asElement().isBoolean()) {
                scriptEntry.addObject("ssl", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("sqlid")) {
            throw new InvalidArgumentsException("Must specify an ID!");
        }
        if (!scriptEntry.hasObject("ssl")) {
            scriptEntry.defaultObject("ssl", new ElementTag("false"));
        }
        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify an action!");
        }
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag action = scriptEntry.getElement("action");
        final ElementTag server = scriptEntry.getElement("server");
        final ElementTag username = scriptEntry.getElement("username");
        final ElementTag password = scriptEntry.getElement("password");
        final ElementTag passwordFile = scriptEntry.getElement("passwordfile");
        final ElementTag ssl = scriptEntry.getElement("ssl");
        final ElementTag sqlID = scriptEntry.getElement("sqlid");
        final ElementTag query = scriptEntry.getElement("query");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), sqlID, action, server, username, passwordFile, query);
        }
        if (!action.asString().equalsIgnoreCase("connect") &&
                (!action.asString().equalsIgnoreCase("query") || !scriptEntry.shouldWaitFor())) {
            scriptEntry.setFinished(true);
        }
        try {
            if (action.asString().equalsIgnoreCase("connect")) {
                if (server == null) {
                    Debug.echoError(scriptEntry, "Must specify a server!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (username == null) {
                    Debug.echoError(scriptEntry, "Must specify a username!");
                    scriptEntry.setFinished(true);
                    return;
                }
                String passwordRaw;
                if (password != null) {
                    passwordRaw = password.asString();
                }
                else {
                    if (passwordFile == null) {
                        Debug.echoError(scriptEntry, "Must specify a password!");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    File f = new File(DenizenCore.implementation.getDataFolder(), passwordFile.asString());
                    if (!DenizenCore.implementation.canReadFile(f)) {
                        Debug.echoError(scriptEntry, "Cannot read from that file path due to security settings in Denizen/config.yml.");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    if (!f.exists()) {
                        Debug.echoError(scriptEntry, "Invalid passwordfile specified. File does not exist.");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    passwordRaw = CoreUtilities.journallingLoadFile(f.getAbsolutePath());
                    if (passwordRaw == null || passwordRaw.length() < 2 || passwordRaw.length() > 200) {
                        Debug.echoError(scriptEntry, "Invalid passwordfile specified. File content doesn't look like a password.");
                        scriptEntry.setFinished(true);
                        return;
                    }
                    passwordRaw = passwordRaw.trim();
                }
                if (connections.containsKey(sqlID.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Already connected to a server with ID '" + sqlID.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                final String passwordToUse = passwordRaw;
                DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(() -> {
                    Connection con = null;
                    if (Debug.verbose) {
                        Debug.echoDebug(scriptEntry, "Connecting to " + server.asString());
                    }
                    try {
                        con = getConnection(username.asString(), passwordToUse, server.asString(), ssl.asString());
                    }
                    catch (final Exception e) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry.getResidingQueue(), "SQL Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (Debug.verbose) {
                                Debug.echoError(scriptEntry.getResidingQueue(), e);
                            }
                        }, 0));
                    }
                    if (Debug.verbose) {
                        Debug.echoDebug(scriptEntry, "Connection did not error");
                    }
                    final Connection conn = con;
                    if (con != null) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            connections.put(sqlID.asString().toUpperCase(), conn);
                            Debug.echoDebug(scriptEntry, "Successfully connected to " + server);
                            scriptEntry.setFinished(true);
                        }, 0));
                    }
                    else {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            scriptEntry.setFinished(true);
                            if (Debug.verbose) {
                                Debug.echoDebug(scriptEntry, "Connecting errored!");
                            }
                        }, 0));
                    }
                }, 0)));
            }
            else if (action.asString().equalsIgnoreCase("disconnect")) {
                Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                con.close();
                connections.remove(sqlID.asString().toUpperCase());
                Debug.echoDebug(scriptEntry, "Disconnected from '" + sqlID.asString() + "'.");
            }
            else if (action.asString().equalsIgnoreCase("query")) {
                if (query == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify a query!");
                    scriptEntry.setFinished(true);
                    return;
                }
                final Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Debug.echoDebug(scriptEntry, "Running query " + query.asString());
                Runnable doQuery = () -> {
                    try {
                        Statement statement = con.createStatement();
                        ResultSet set = statement.executeQuery(query.asString());
                        ResultSetMetaData rsmd = set.getMetaData();
                        final int columns = rsmd.getColumnCount();
                        int count = 0;
                        ListTag rows = new ListTag();
                        ListTag resultList = new ListTag();
                        while (set.next()) {
                            count++;
                            StringBuilder current = new StringBuilder();
                            ListTag subList = new ListTag();
                            for (int i = 0; i < columns; i++) {
                                current.append(EscapeTagBase.escape(set.getString(i + 1))).append("/");
                                subList.addObject(new ElementTag(set.getString(i + 1)));
                            }
                            rows.add(current.toString());
                            resultList.addObject(subList);
                        }
                        scriptEntry.addObject("result", rows);
                        scriptEntry.addObject("result_list", resultList);
                        final int finalCount = count;
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoDebug(scriptEntry, "Got a query result of " + columns + " columns and " + finalCount + " rows");
                            scriptEntry.setFinished(true);
                        }, 0));
                    }
                    catch (final Exception ex) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry.getResidingQueue(), "SQL Exception: " + ex.getMessage());
                            scriptEntry.setFinished(true);
                            if (Debug.verbose) {
                                Debug.echoError(scriptEntry.getResidingQueue(), ex);
                            }
                        }, 0));
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(doQuery, 0)));
                }
                else {
                    doQuery.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("update")) {
                if (query == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify an update query!");
                    scriptEntry.setFinished(true);
                    return;
                }
                final Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Debug.echoDebug(scriptEntry, "Running update " + query.asString());
                Runnable doUpdate = () -> {
                    try {
                        Statement statement = con.createStatement();
                        int affected = statement.executeUpdate(query.asString(), Statement.RETURN_GENERATED_KEYS);
                        scriptEntry.addObject("affected_rows", new ElementTag(affected));
                        ResultSet set = statement.getGeneratedKeys();
                        ResultSetMetaData rsmd = set.getMetaData();
                        int columns = rsmd.getColumnCount();
                        ListTag rows = new ListTag();
                        ListTag resultList = new ListTag();
                        while (set.next()) {
                            StringBuilder current = new StringBuilder();
                            ListTag subList = new ListTag();
                            for (int i = 0; i < columns; i++) {
                                current.append(EscapeTagBase.escape(set.getString(i + 1))).append("/");
                                subList.addObject(new ElementTag(set.getString(i + 1)));
                            }
                            rows.add(current.toString());
                            resultList.addObject(subList);
                        }
                        scriptEntry.addObject("result", rows);
                        scriptEntry.addObject("result_list", resultList);
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoDebug(scriptEntry, "Got a query result of " + columns + " columns");
                            Debug.echoDebug(scriptEntry, "Updated " + affected + " rows");
                            scriptEntry.setFinished(true);
                        }, 0));
                    }
                    catch (Exception ex) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry.getResidingQueue(), "SQL Exception: " + ex.getMessage());
                            if (Debug.verbose) {
                                Debug.echoError(scriptEntry.getResidingQueue(), ex);
                            }
                        }, 0));
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(doUpdate, 0)));
                }
                else {
                    doUpdate.run();
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Unknown action '" + action.asString() + "'");
            }
        }
        catch (SQLException ex) {
            Debug.echoError(scriptEntry.getResidingQueue(), "SQL Exception: " + ex.getMessage());
            if (Debug.verbose) {
                Debug.echoError(scriptEntry.getResidingQueue(), ex);
            }
        }
    }

    public Connection getConnection(String userName, String password, String server, String ssl) throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", userName);
        connectionProps.put("password", password);
        connectionProps.put("useSSL", ssl);
        connectionProps.put("LoginTimeout", "7");
        if (!server.contains("://")) {
            // This is a weird hack that the internet recommends to guarantee the MySQL driver will be registered
            // and has been updated for MySQL class renames
            // Java and SQL are awful, this is a mess, how is this considered acceptable normal code practices that are literally officially recommended now???
            try {
                Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            }
            catch (Throwable ex) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                }
                catch (Throwable ex2) {
                    Debug.echoError(ex2);
                }
            }
            server = "mysql://" + server;
        }
        return DriverManager.getConnection("jdbc:" + server, connectionProps);
    }
}
