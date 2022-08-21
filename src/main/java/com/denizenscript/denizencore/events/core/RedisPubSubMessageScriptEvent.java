package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class RedisPubSubMessageScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // redis pubsub message
    //
    // @Switch channel:<channel> to only fire on events advanced-matching the given channel.
    //
    // @Group Core
    //
    // @Triggers when a subscribed redis connection receives a published message, see <@link command Redis>.
    //
    // @Context
    // <context.redis_id> returns the connection id that saw this message.
    // <context.pattern> returns the redis pattern that matched the channel.
    // <context.channel> returns the actual channel matched.
    // <context.message> returns the published message.
    //
    // -->

    public static RedisPubSubMessageScriptEvent instance;

    public String redisID;
    public String pattern;
    public String channel;
    public String message;

    public RedisPubSubMessageScriptEvent() {
        instance = this;
        registerCouldMatcher("redis pubsub message");
        registerSwitches("channel");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "channel", channel)) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "redis_id": return new ElementTag(redisID);
            case "pattern": return new ElementTag(pattern);
            case "channel": return new ElementTag(channel);
            case "message": return new ElementTag(message);
        }
        return super.getContext(name);
    }

    public void handle(String redisID, String pattern, String channel, String message) {
        this.redisID = redisID;
        this.pattern = pattern;
        this.channel = channel;
        this.message = message;
        fire();
    }
}
