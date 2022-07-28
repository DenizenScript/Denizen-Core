package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DenizenJedisPubSub;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.SafeEncoder;

import java.util.*;

public class RedisCommand extends AbstractCommand implements Holdable {

    public RedisCommand() {
        setName("redis");
        setSyntax("redis [id:<ID>] [connect:<host> (auth:<secret>) (port:<port>/{6379}) (ssl:true/{false})/disconnect/subscribe:<channel>|.../unsubscribe/publish:<channel> message:<message>/command:<command> (args:<arg>|...)]");
        setRequiredArguments(2, 5);
        isProcedural = false;
        setPrefixesHandled("auth", "port", "id", "message", "args");
        setBooleansHandled("ssl");
    }

    // <--[command]
    // @Name Redis
    // @Syntax redis [id:<ID>] [connect:<host> (auth:<secret>) (port:<port>/{6379}) (ssl:true/{false})/disconnect/subscribe:<channel>|.../unsubscribe/publish:<channel> message:<message>/command:<command> (args:<arg>|...)]
    // @Required 2
    // @Maximum 5
    // @Short Interacts with a Redis server.
    // @Group core
    //
    // @Description
    // This command is used to interact with a redis server. It can run any standard redis commands as well as subscribe for pub/sub redis channel notifications.
    //
    // Redis is a simple key/value data store that is typically used for caching and sending data between servers.
    // The redis server runs in memory, meaning requests are insanely fast. If you run redis locally, you can expect responses to take under a millisecond.
    // It is normally advised to run commands as ~waitable (see <@link language ~waitable>), but because of the usual fast responses when the server is on localhost, you can also run commands without ~waiting.
    //
    // When running commands, make sure to escape unpredictable values such as player input.
    // Alternatively, include the main redis command as the 'command' input and further arguments as a ListTag input for 'args'.
    //
    // This command supports subscribing to pub/sub redis channels. This allows you to listen to published messages to redis from any source, including other servers.
    // When you subscribe to a channel, matching messages sent to the channel will trigger the <@link event redis pubsub message> event.
    // Connections that are subscribed to channels get tied up listening for messages and are unavailable to run redis commands.
    // The channels you subscribe to support wildcard (*) matchers and other patterns, defined by the redis docs: <@link url https://redis.io/commands/psubscribe>
    //
    // Note: Make sure there are at least a few ticks between opening a subscription and closing it, otherwise strange behavior will occur.
    //
    // You can publish messages to listening subscribers via publish:<channel> message:<message>.
    // Note that this has to be done on a separate redis connection if it is already subscribed.
    // Saving the result of this call returns the number of connected subscribers the message was sent to.
    //
    // The redis command is merely a wrapper, and further usage details should be gathered from an official redis command reference rather than from Denizen command help.
    // You can view the official redis documentation and the supported commands here: <@link url https://redis.io/>
    //
    // @Tags
    // <entry[saveName].result> returns an ElementTag or ListTag of the results of your command, depending on the redis command you ran.
    // <util.redis_connections>
    //
    // @Usage
    // Use to connect to a Redis server.
    // - ~redis id:name connect:localhost
    //
    // @Usage
    // Use to connect to a Redis server with a secret auth key.
    // - ~redis id:name connect:localhost auth:<secret[my_redis_secret]>
    //
    // @Usage
    // Use to connect to a Redis server over ssl.
    // - ~redis id:name connect:localhost port:6380 ssl:true
    //
    // @Usage
    // Set a key/value pair in the Redis server.
    // - ~redis id:name "command:set my_key my_value"
    //
    // @Usage
    // Delete the "foo" key.
    // - ~redis id:name "command:del my_key"
    //
    // @Usage
    // Set a key that auto-expires in 60 seconds.
    // - ~redis id:name "command:setex my_key 60 'value with spaces'"
    //
    // @Usage
    // Run a command with unpredictable input.
    // - ~redis id:name command:set args:<list[my_key].include_single[<context.message>]>
    //
    // @Usage
    // Get a key's value.
    // - ~redis id:name "command:get my_key" save:result
    //
    // @Usage
    // Get a key's value in the background via a waitable.
    // - ~redis id:name "command:get my_key" save:result
    //
    // @Usage
    // Append values to the front or back of a redis list.
    // - ~redis id:name "command:rpush my_list a"
    // - ~redis id:name "command:rpush my_list b"
    // - ~redis id:name "command:lpush my_list c"
    //
    // @Usage
    // Retrieve a ListTag of the members stored in a redis list (0 is the start of the list, -1 is the end).
    // - ~redis id:name "command:lrange my_list 0 -1"
    //
    // @Usage
    // Subscribe to a redis channel. This will match published messages to channel_1, channel_foo, etc.
    // - ~redis id:name subscribe:channel_*
    //
    // @Usage
    // Subscribe to multiple redis channels. Supports wildcards for any list entry.
    // - ~redis id:name subscribe:a|b*|c|d
    //
    // @Usage
    // Publish a message to a redis channel. This will trigger the <@link event redis pubsub message> event for any subscribed connections for any server.
    // - ~redis id:name publish:channel_1 "message:hey look something happened"
    //
    // @Usage
    // Unsubscribe from a redis channel. Leaves the connection intact.
    // - redis id:name unsubscribe
    //
    // @Usage
    // Disconnect from redis.
    // - redis id:name disconnect
    // -->

    public static Map<String, Jedis> connections = new HashMap<>();
    public static Map<String, JedisPubSub> subscriptions = new HashMap<>();

    @Override
    public void onDisable() {
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

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("connect")) {
                scriptEntry.addObject("action", new ElementTag("connect"));
                scriptEntry.addObject("host", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matches("disconnect")) {
                scriptEntry.addObject("action", new ElementTag("disconnect"));
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("subscribe")) {
                scriptEntry.addObject("action", new ElementTag("subscribe"));
                scriptEntry.addObject("channels", arg.asType(ListTag.class));
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matches("unsubscribe")) {
                scriptEntry.addObject("action", new ElementTag("unsubscribe"));
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("publish")) {
                scriptEntry.addObject("action", new ElementTag("publish"));
                scriptEntry.addObject("channel", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action")
                    && arg.matchesPrefix("command")) {
                scriptEntry.addObject("action", new ElementTag("command"));
                scriptEntry.addObject("command", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify a valid redis action!");
        }
    }

    public ObjectTag processResponse(Object response) {
        if (response instanceof List) {
            ListTag list = new ListTag();
            for (Object o : (List) response) {
                list.addObject(processResponse(o));
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

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        if (!CoreConfiguration.allowRedis) {
            Debug.echoError(scriptEntry, "Redis disabled by config!");
            return;
        }
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
        String redisID = CoreUtilities.toLowerCase(id.asString());
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), id, action, host, auth, port, db("ssl", ssl), channels, channel, message, command, args);
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
                DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(() -> {
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
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + e.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, e);
                            }
                        }, 0));
                    }
                    if (CoreConfiguration.debugVerbose) {
                        Debug.echoDebug(scriptEntry, "Connection did not error");
                    }
                    final Jedis conn = con;
                    if (con != null) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            connections.put(redisID, conn);
                            Debug.echoDebug(scriptEntry, "Successfully connected to " + host + " on port " + port);
                            scriptEntry.setFinished(true);
                        }, 0));
                    }
                    else {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoDebug(scriptEntry, "Connecting errored!");
                            }
                        }, 0));
                    }
                }, 0)));
            }
            else if (action.asString().equalsIgnoreCase("disconnect")) {
                scriptEntry.setFinished(true);
                if (!connections.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
                    return;
                }
                Jedis con = connections.remove(redisID);
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
                Jedis con = connections.get(redisID);
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
                    scriptEntry.setFinished(true);
                    return;
                }
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
                Thread thr = new Thread(() -> { con.psubscribe(jedisPubSub, channelArr); scriptEntry.setFinished(true); });
                thr.start();
            }
            else if (action.asString().equalsIgnoreCase("unsubscribe")) {
                scriptEntry.setFinished(true);
                if (!connections.containsKey(redisID)) {
                    Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
                    return;
                }
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
                final Jedis con = connections.get(redisID);
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
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
                        ElementTag result = new ElementTag(con.publish(CoreUtilities.toLowerCase(channel.asString()), message.asString()));
                        scriptEntry.addObject("result", result);
                        scriptEntry.setFinished(true);
                    }
                    catch (final Exception ex) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + ex.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, ex);
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
            else if (action.asString().equalsIgnoreCase("command")) {
                if (command == null) {
                    Debug.echoError(scriptEntry, "Must specify a valid redis command!");
                    scriptEntry.setFinished(true);
                    return;
                }
                final Jedis con = connections.get(redisID);
                if (con == null) {
                    Debug.echoError(scriptEntry, "Not connected to redis server with ID '" + redisID + "'!");
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
                            String[] splitCommand = ArgumentHelper.buildArgs(command.asString());
                            redisCommand = splitCommand[0];
                            redisArgs = Arrays.copyOfRange(splitCommand, 1, splitCommand.length);
                        }
                        else {
                            redisCommand = command.asString();
                            redisArgs = args.toArray(new String[0]);
                        }
                        ObjectTag result = processResponse(con.sendCommand(() -> SafeEncoder.encode(redisCommand), redisArgs));
                        scriptEntry.addObject("result", result);
                        scriptEntry.setFinished(true);
                    }
                    catch (final Exception ex) {
                        DenizenCore.schedule(new OneTimeSchedulable(() -> {
                            Debug.echoError(scriptEntry, "Redis Exception: " + ex.getMessage());
                            scriptEntry.setFinished(true);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.echoError(scriptEntry, ex);
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
