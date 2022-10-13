package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.RedisPubSubMessageScriptEvent;
import redis.clients.jedis.JedisPubSub;

public class DenizenJedisPubSub extends JedisPubSub {

    public String connID;

    public DenizenJedisPubSub(String connID) {
        this.connID = connID;
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        DenizenCore.runOnMainThread(() -> {
            RedisPubSubMessageScriptEvent.instance.handle(this.connID, CoreUtilities.toLowerCase(pattern), CoreUtilities.toLowerCase(channel), message);
        });
    }
}
