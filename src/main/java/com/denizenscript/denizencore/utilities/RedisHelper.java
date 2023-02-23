package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisHelper {

    public static Map<String, Jedis> connections = new HashMap<>();
    public static Map<String, JedisPubSub> subscriptions = new HashMap<>();

    public static AtomicBoolean isEnabled = new AtomicBoolean(true);

    public static void onDisable() {
        isEnabled.set(false);
        for (Map.Entry<String, JedisPubSub> entry : subscriptions.entrySet()) {
            try {
                entry.getValue().punsubscribe();
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }
        subscriptions.clear();
        for (Map.Entry<String, Jedis> entry : connections.entrySet()) {
            try {
                entry.getValue().close();
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }
        connections.clear();
    }

    public static ObjectTag processResponse(Object response) {
        if (response instanceof List) {
            ListTag list = new ListTag();
            for (Object o : (List) response) {
                ObjectTag resp = processResponse(o);
                if (resp == null) {
                    resp = new ElementTag("null");
                }
                list.addObject(resp);
            }
            return list;
        }
        else if (response instanceof byte[]) {
            return new ElementTag(new String((byte[]) response));
        }
        else if (response instanceof Long) {
            return new ElementTag((long) response);
        }
        else {
            return null;
        }
    }

    public static void runChecked(Runnable r, ScriptEntry scriptEntry) {
        DenizenCore.runAsync(() -> {
            try {
                r.run();
            }
            catch (Throwable ex) {
                if (isEnabled.get() || CoreConfiguration.debugVerbose) { // Ignore errors when server is shutting down
                    DenizenCore.runOnMainThread(() -> {
                        Debug.echoError(ex);
                        scriptEntry.setFinished(true);
                    });
                }
            }
        });
    }

    public static void executeCommand(ScriptEntry scriptEntry) {
        isEnabled.set(true);
        ElementTag port = scriptEntry.argForPrefixAsElement("port", "6379");
        if (!port.isInt()) {
            throw new InvalidArgumentsRuntimeException("Port must be an integer number.");
        }
        ElementTag id = scriptEntry.requiredArgForPrefixAsElement("id");
        ElementTag message = scriptEntry.argForPrefixAsElement("message", null);
        ListTag args = scriptEntry.argForPrefix("args", ListTag.class, true);
        boolean ssl = scriptEntry.argAsBoolean("ssl");
        ObjectTag auth = scriptEntry.argForPrefix("auth", ObjectTag.class, true);
        ElementTag action = scriptEntry.getElement("action");
        ElementTag host = scriptEntry.getElement("host");
        ListTag channels = scriptEntry.getObjectTag("channels");
        ElementTag channel = scriptEntry.getElement("channel");
        ElementTag command = scriptEntry.getElement("command");
        String redisID = id.asLowerString();
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, "redis", id, action, host, auth, port, AbstractCommand.db("ssl", ssl), channels, channel, message, command, args);
        }
        if (!action.asString().equalsIgnoreCase("connect") &&
                (!action.asString().equalsIgnoreCase("command") || !scriptEntry.shouldWaitFor())) {
            scriptEntry.setFinished(true);
        }
        try {
            if (action.asString().equalsIgnoreCase("connect")) {
                if (host == null) {
                    Debug.echoError(scriptEntry, "Must specify a valid redis host!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (connections.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Already connected to a server with ID '" + redisID + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                runChecked(() -> {
                    Jedis con = null;
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connecting to " + host + " on port " + port);
                    }
                    try {
                        con = new Jedis(host.asString(), port.asInt(), ssl);
                        if (auth != null) {
                            String[] redisArgs = new String[] { auth.shouldBeType(SecretTag.class) ? auth.asType(SecretTag.class, scriptEntry.context).getValue() : auth.toString() };
                            if (redisArgs[0] == null) {
                                throw new Exception("Invalid SecretTag input for AUTH.");
                            }
                            con.sendCommand(() -> SafeEncoder.encode("AUTH"), redisArgs);
                        }
                    }
                    catch (final Exception e) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        });
                    }
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connection did not error");
                    }
                    final Jedis conn = con;
                    if (con != null) {
                        DenizenCore.runOnMainThread(() -> {
                            connections.put(redisID, conn);
                            Debug.echoDebug(scriptEntry, "Successfully connected to " + host + " on port " + port);
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
                }, scriptEntry);
                return;
            }
            Jedis con = connections.get(redisID);
            if (con == null) {
                Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
                scriptEntry.setFinished(true);
                return;
            }
            if (action.asString().equalsIgnoreCase("disconnect")) {
                scriptEntry.setFinished(true);
                connections.remove(redisID);
                JedisPubSub pubSub = subscriptions.remove(redisID);
                if (pubSub != null) {
                    try {
                        // TODO: Make this work better?
                        pubSub.punsubscribe();
                    }
                    catch (Exception e) {
                        Debug.echoError(e);
                    }
                }
                try {
                    con.close();
                }
                catch (Exception e) {
                    Debug.echoError(e);
                }
                Debug.echoDebug(scriptEntry, "Disconnected from '" + redisID + "'.");
            }
            else if (action.asString().equalsIgnoreCase("subscribe")) {
                if (subscriptions.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Already subscribed to a channel on redis server with ID '" + redisID + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                JedisPubSub jedisPubSub = new DenizenJedisPubSub(redisID);
                subscriptions.put(redisID, jedisPubSub);
                String[] channelArr = new String[channels.size()];
                for (int i = 0; i < channels.size(); i++) {
                    channelArr[i] = CoreUtilities.toLowerCase(channels.get(i));
                }
                runChecked(() -> { con.psubscribe(jedisPubSub, channelArr); scriptEntry.setFinished(true); }, scriptEntry);
            }
            else if (action.asString().equalsIgnoreCase("unsubscribe")) {
                scriptEntry.setFinished(true);
                if (!subscriptions.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Not subscribed to redis server with ID '" + redisID + "'!");
                    return;
                }
                JedisPubSub pubSub = subscriptions.remove(redisID);
                try {
                    pubSub.punsubscribe();
                }
                catch (Exception e) {
                    Debug.echoError(e);
                }
            }
            else if (action.asString().equalsIgnoreCase("publish")) {
                if (message == null) {
                    Debug.echoError(scriptEntry, "Must specify a valid message to publish!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (subscriptions.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Cannot publish messages while subscribed to redis server with ID '" + redisID + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Debug.echoDebug(scriptEntry, "Publishing message '" + message.asString() + "' to channel '" + channel.asString() + "'");
                Runnable doQuery = () -> {
                    try {
                        ElementTag result = new ElementTag(con.publish(channel.asLowerString(), message.asString()));
                        scriptEntry.saveObject("result", result);
                        scriptEntry.setFinished(true);
                    }
                    catch (final Exception ex) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + ex.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, ex);
                            }
                        });
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    runChecked(doQuery, scriptEntry);
                }
                else {
                    doQuery.run();
                }
            }
            else if (action.asString().equalsIgnoreCase("command")) {
                if (command == null) {
                    Debug.echoError(scriptEntry, "Must specify a valid redis command!");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (subscriptions.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Cannot run commands while subscribed to redis server with ID '" + redisID + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
                Debug.echoDebug(scriptEntry, "Running command " + command.asString());
                Runnable doQuery = () -> {
                    try {
                        String redisCommand;
                        String[] redisArgs;
                        if (args == null) {
                            String[] splitCommand = ArgumentHelper.buildArgs(command.asString(), false);
                            redisCommand = splitCommand[0];
                            redisArgs = Arrays.copyOfRange(splitCommand, 1, splitCommand.length);
                        }
                        else {
                            redisCommand = command.asString();
                            redisArgs = args.toArray(new String[0]);
                        }
                        ObjectTag result = processResponse(con.sendCommand(() -> SafeEncoder.encode(redisCommand), redisArgs));
                        scriptEntry.saveObject("result", result);
                        scriptEntry.setFinished(true);
                    }
                    catch (final Exception ex) {
                        DenizenCore.runOnMainThread(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + ex.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, ex);
                            }
                        });
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    runChecked(doQuery, scriptEntry);
                }
                else {
                    doQuery.run();
                }
            }
            else {
                Debug.echoError(scriptEntry, "Unknown action '" + action.asString() + "'");
            }
        }
        catch (Exception ex) {
            Debug.echoError(scriptEntry, "Redis Exception: " + ex.getMessage());
            if (CoreConfiguration.debugVerbose) {
                Debug.echoError(scriptEntry, ex);
            }
        }
    }
}
