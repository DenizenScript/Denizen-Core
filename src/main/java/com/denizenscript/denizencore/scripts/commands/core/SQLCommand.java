package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SQLCommand extends AbstractCommand implements Holdable {

    public SQLCommand() {
        setName("sql");
        setSyntax("sql [id:<ID>] [disconnect/connect:<server> (username:<username>) (password:<password>) (ssl:true/{false})/query:<query>/update:<update>]");
        setRequiredArguments(2, 5);
        isProcedural = false;
    }

    // <--[command]
    // @Name SQL
    // @Syntax sql [id:<ID>] [disconnect/connect:<server> (username:<username>) (password:<password>) (ssl:true/{false})/query:<query>/update:<update>]
    // @Required 2
    // @Maximum 5
    // @Short Interacts with a MySQL server.
    // @Group core
    //
    // @Description
    // This command is used to interact with a MySQL server. It can update the database or query it for information.
    // The general usage order is connect -> update/query -> disconnect.
    // It is not required that you disconnect right after using, and in fact encouraged that you keep a connection open where possible.
    // When connecting, the server format is IP:Port/Database, EG 'localhost:3306/test'.
    // You can switch whether SSL is used for the connection (defaults to false).
    // Note that when using tag, it is recommended you escape unusual inputs to avoid SQL injection.
    // The SQL command is merely a wrapper for SQL queries, and further usage details should be gathered from an official
    // MySQL query reference rather than from Denizen command help.
    // SQL connections are not instant - they can take several seconds, or just never connect at all.
    // It is recommended you hold the command by doing "- ~sql ..." rather than just "- sql ..."
    // as this will delay the commands following the SQL command until after the SQL operation is complete.
    //
    // @Tags
    // <entry[saveName].result> returns a ListTag of all rows from a query or update command, of the form escaped_text/escaped_text|escaped_text/escaped_text
    // <entry[saveName].affected_rows> returns how many rows were affected by an update command.
    //
    // @Usage
    // Use to connect to an SQL server.
    // - ~sql id:name connect:localhost:3306/test username:space password:space
    //
    // @Usage
    // Use to connect to an SQL server over an SSL connection.
    // - ~sql id:name connect:localhost:3306/test username:space password:space ssl:true
    //
    // @Usage
    // Use to connect to an SQL server with a UTF8 text encoding.
    // - ~sql id:name connect:localhost:3306/test?characterEncoding=utf8 username:space password:space
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

        for (Argument arg : scriptEntry.getProcessedArgs()) {

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
        final ElementTag ssl = scriptEntry.getElement("ssl");
        final ElementTag sqlID = scriptEntry.getElement("sqlid");
        final ElementTag query = scriptEntry.getElement("query");

        if (scriptEntry.dbCallShouldDebug()) {

            Debug.report(scriptEntry, getName(), sqlID.debug()
                    + action.debug()
                    + (server != null ? server.debug() : "")
                    + (username != null ? username.debug() : "")
                    + (password != null ? ArgumentHelper.debugObj("password", "NotLogged") : "")
                    + (query != null ? query.debug() : ""));

        }

        if (!action.asString().equalsIgnoreCase("connect") &&
                (!action.asString().equalsIgnoreCase("query") || !scriptEntry.shouldWaitFor())) {
            scriptEntry.setFinished(true);
        }

        try {
            if (action.asString().equalsIgnoreCase("connect")) {
                if (server == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify a server!");
                    return;
                }
                if (username == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify a username!");
                    return;
                }
                if (password == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify a password!");
                    return;
                }
                if (connections.containsKey(sqlID.asString().toUpperCase())) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Already connected to a server with ID '" + sqlID.asString() + "'!");
                    return;
                }
                DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(new Runnable() {
                    @Override
                    public void run() {
                        Connection con = null;
                        if (Debug.verbose) {
                            Debug.echoDebug(scriptEntry, "Connecting to " + server.asString());
                        }
                        try {
                            con = getConnection(username.asString(), password.asString(), server.asString(), ssl.asString());
                        }
                        catch (final Exception e) {
                            DenizenCore.schedule(new OneTimeSchedulable(new Runnable() {
                                @Override
                                public void run() {
                                    Debug.echoError(scriptEntry.getResidingQueue(), "SQL Exception: " + e.getMessage());
                                    scriptEntry.setFinished(true);
                                    if (Debug.verbose) {
                                        Debug.echoError(scriptEntry.getResidingQueue(), e);
                                    }
                                }
                            }, 0));
                        }
                        if (Debug.verbose) {
                            Debug.echoDebug(scriptEntry, "Connection did not error");
                        }
                        final Connection conn = con;
                        if (con != null) {
                            DenizenCore.schedule(new OneTimeSchedulable(new Runnable() {
                                @Override
                                public void run() {
                                    connections.put(sqlID.asString().toUpperCase(), conn);
                                    Debug.echoDebug(scriptEntry, "Successfully connected to " + server);
                                    scriptEntry.setFinished(true);
                                }
                            }, 0));
                        }
                        else {
                            DenizenCore.schedule(new OneTimeSchedulable(new Runnable() {
                                @Override
                                public void run() {
                                    scriptEntry.setFinished(true);
                                    if (Debug.verbose) {
                                        Debug.echoDebug(scriptEntry, "Connecting errored!");
                                    }
                                }
                            }, 0));
                        }
                    }
                }, 0)));
            }
            else if (action.asString().equalsIgnoreCase("disconnect")) {
                Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
                    return;
                }
                con.close();
                connections.remove(sqlID.asString().toUpperCase());
                Debug.echoDebug(scriptEntry, "Disconnected from '" + sqlID.asString() + "'.");
            }
            else if (action.asString().equalsIgnoreCase("query")) {
                if (query == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Must specify a query!");
                    return;
                }
                final Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
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
                        while (set.next()) {
                            count++;
                            StringBuilder current = new StringBuilder();
                            for (int i = 0; i < columns; i++) {
                                current.append(EscapeTagBase.escape(set.getString(i + 1))).append("/");
                            }
                            rows.add(current.toString());
                        }
                        scriptEntry.addObject("result", rows);
                        final int finalCount = count;
                        DenizenCore.schedule(new OneTimeSchedulable(new Runnable() {
                            @Override
                            public void run() {
                                Debug.echoDebug(scriptEntry, "Got a query result of " + columns + " columns and " + finalCount + " rows");
                                scriptEntry.setFinished(true);
                            }
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
                    return;
                }
                final Connection con = connections.get(sqlID.asString().toUpperCase());
                if (con == null) {
                    Debug.echoError(scriptEntry.getResidingQueue(), "Not connected to server with ID '" + sqlID.asString() + "'!");
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
                        while (set.next()) {
                            StringBuilder current = new StringBuilder();
                            for (int i = 0; i < columns; i++) {
                                current.append(EscapeTagBase.escape(set.getString(i + 1))).append("/");
                            }
                            rows.add(current.toString());
                        }
                        scriptEntry.addObject("result", rows);
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
        return DriverManager.getConnection("jdbc:mysql://" + server, connectionProps);
    }
}
