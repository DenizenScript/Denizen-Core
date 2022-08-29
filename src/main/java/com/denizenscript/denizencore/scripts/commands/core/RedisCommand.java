package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.RedisHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;

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

    @Override
    public void onDisable() {
        if (everUsed) {
            RedisHelper.onDisable();
        }
    }

    public static volatile boolean everUsed = false;

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

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        if (!CoreConfiguration.allowRedis) {
            Debug.echoError(scriptEntry, "Redis disabled by config!");
            return;
        }
        everUsed = true;
        RedisHelper.executeCommand(scriptEntry);
    }
}
