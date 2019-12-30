package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.scripts.commands.core.*;
import com.denizenscript.denizencore.scripts.commands.file.LogCommand;
import com.denizenscript.denizencore.scripts.commands.queue.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.commands.file.FileCopyCommand;
import com.denizenscript.denizencore.scripts.commands.file.YamlCommand;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandRegistry {

    public CommandRegistry() {
    }

    public final Map<String, AbstractCommand> instances = new HashMap<>();
    public final Map<Class<? extends AbstractCommand>, String> classes = new HashMap<>();

    public boolean register(String commandName, AbstractCommand commandInstance) {
        this.instances.put(CoreUtilities.toLowerCase(commandName), commandInstance);
        this.classes.put(((AbstractCommand) commandInstance).getClass(), CoreUtilities.toLowerCase(commandName));
        return true;
    }

    public Map<String, AbstractCommand> list() {
        return instances;
    }

    public AbstractCommand get(String commandName) {
        return instances.get(CoreUtilities.toLowerCase(commandName));
    }

    public <T extends AbstractCommand> T get(Class<T> clazz) {
        String command = classes.get(clazz);
        if (command != null) {
            return clazz.cast(instances.get(command));
        }
        else {
            return null;
        }
    }

    // <--[language]
    // @Name Command Syntax
    // @group Script Command System
    // @Description
    // Almost every Denizen command and requirement has arguments after the command itself.
    // These arguments are just snippets of text showing what exactly the command should do,
    // like what the chat command should say, or where the look command should point.
    // But how do you know what to put in the arguments?
    //
    // You merely need to look at the command's usage/syntax info.
    // Let's take for example:
    // <code>
    // - animatechest [<location>] ({open}/close) (sound:{true}/false)
    // </code>
    // Obviously, the command is 'animatechest'... but what does the rest of it mean?
    //
    // Anything in [brackets] is required... you MUST put it there.
    // Anything in (parenthesis) is optional... you only need to put it there if you want to.
    // Anything in {braces} is default... the command will just assume this if no argument is actually typed.
    // Anything in <> is non-literal... you must change what is inside of it.
    // Anything outside of <> is literal... you must put it exactly as-is.
    // <#> represents a number without a decimal, and <#.#> represents a number with a decimal
    // Lastly, input that ends with "|..." (EG, [<entity>|...] ) can take a list of the input indicated before it (In that example, a list of entities)
    // An argument that contains a ":" (like "duration:<value>") is a prefix:value pair. The prefix is usually literal and the value dynamic. The prefix and the colon should be kept directly in the final command.
    //
    // A few examples:
    // [<location>] is required and non-literal... you might fill it with a notable location, or a tag that returns one like '<player.location>'.
    // (sound:{true}/false) is optional and has a default value of true... you can put sound:false to prevent sound, or leave it blank to allow sound.
    // (repeats:<#>) is optional, has no clear default, and is a number. You can put repeats:3 to repeat three times, or leave it blank to not repeat.
    // Note: Optional arguments without a default usually have a secret default... EG, the (repeats:<#>) above has a secret default of '0'.
    //
    // Also, you should never directly type in [], (), {}, or <> even though they are in the syntax info.
    // The only exception is in a replaceable tag (EG: <npc.has_trait[<traitname>]> will take <npc.has_trait[mytrait]> as a valid actual usage)
    //
    // Highly specific note: <commands> means a block of commands wrapped in braces or as a sub-block... EG:
    // <code>
    // - repeat 3:
    //   - narrate "<[value]>"
    //   - narrate "everything spaced out as a sub-block (these two narrates) following a ":" ended command (that repeat) is for the <commands> input!"
    // </code>
    //
    // -->

    public void registerCoreCommands() {

        registerCoreMember(AdjustCommand.class, "ADJUST", "adjust [<ObjectTag>/def:<name>|...] [<mechanism>](:<value>)", 2);
        registerCoreMember(AsyncCommand.class, "ASYNC", "async [<commands>]", 0);
        registerCoreMember(ChooseCommand.class, "CHOOSE", "choose [<option>] [<cases>]", 1);
        registerCoreMember(DebugCommand.class, "DEBUG", "debug [<type>] [<message>] (name:<name>)", 2);
        registerCoreMember(DefineCommand.class, "DEFINE", "define [<id>] [<value>]", 1);
        registerCoreMember(DetermineCommand.class, "DETERMINE", "determine (passively) [<value>]", 1);
        registerCoreMember(ElseCommand.class, "ELSE", "else (if <comparison logic>)", 0);
        registerCoreMember(EventCommand.class, "EVENT", "event [<event name>|...] (context:<name>|<object>|...)", 1);
        registerCoreMember(FileCopyCommand.class, "FILECOPY", "filecopy [origin:<origin>] [destination:<destination>] (overwrite)", 2);
        registerCoreMember(ForeachCommand.class, "FOREACH", "foreach [stop/next/<object>|...] (as:<name>) [<commands>]", 1);
        registerCoreMember(GotoCommand.class, "GOTO", "goto [<name>]", 1);
        registerCoreMember(IfCommand.class, "IF", "if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>]", 1);
        registerCoreMember(InjectCommand.class, "INJECT", "inject (locally) [<script>] (path:<name>) (instantly)", 1);
        registerCoreMember(LogCommand.class, "LOG", "log [<text>] (type:{info}/severe/warning/fine/finer/finest/none/clear) [file:<name>]", 2);
        registerCoreMember(MarkCommand.class, "MARK", "mark [<name>]", 1);
        registerCoreMember(QueueCommand.class, "QUEUE", "queue (<queue>) [clear/stop/pause/resume/delay:<duration>]", 1);
        registerCoreMember(RandomCommand.class, "RANDOM", "random [<commands>]", 0);
        registerCoreMember(RateLimitCommand.class, "RATELIMIT", "ratelimit [<object>] [<duration>]", 2);
        registerCoreMember(ReloadCommand.class, "RELOAD", "reload", 0);
        registerCoreMember(RepeatCommand.class, "REPEAT", "repeat [stop/next/<amount>] [<commands>] (as:<name>)", 1);
        registerCoreMember(RunCommand.class, "RUN", "run [<script>/locally] (path:<name>) (def:<element>|...) (id:<name>) (speed:<value>/instantly) (delay:<value>)", 1);
        registerCoreMember(SQLCommand.class, "SQL", "sql [id:<ID>] [disconnect/connect:<server> (username:<username>) (password:<password>) (ssl:true/{false})/query:<query>/update:<update>]", 2);
        registerCoreMember(StopCommand.class, "STOP", "stop", 0);
        registerCoreMember(SyncCommand.class, "SYNC", "sync [<commands>]", 0);
        registerCoreMember(WaitCommand.class, "WAIT", "wait (<duration>) (queue:<name>)", 0);
        registerCoreMember(WaitUntilCommand.class, "WAITUNTIL", "waituntil (rate:<duration>) [<comparisons>]", 1);
        registerCoreMember(WebGetCommand.class, "WEBGET", "webget [<url>] (post:<data>) (headers:<header>/<value>|...) (timeout:<duration>/{10s}) (savefile:<path>)", 1);
        registerCoreMember(WhileCommand.class, "WHILE", "while [stop/next/<comparison tag>] [<commands>]", 1);
        registerCoreMember(YamlCommand.class, "YAML", "yaml [create]/[load:<file>]/[loadtext:<text>]/[unload]/[savefile:<file>]/[copykey:<source key> <target key> (to_id:<name>)]/[set <key>([<#>])(:<action>):<value>] [id:<name>]", 2);
    }

    public <T extends AbstractCommand> void registerCoreMember(Class<T> cmd, String names, String hint, int args) {
        for (String name : names.split(", ")) {

            try {
                cmd.newInstance().activate().as(name).withOptions(hint, args);
            }
            catch (Throwable e) {
                Debug.echoError("Could not register command " + name + ": " + e.getMessage());
                Debug.echoError(e);
            }
        }
    }

    public void disableCoreMembers() {
        for (AbstractCommand member : instances.values()) {
            try {
                member.onDisable();
            }
            catch (Exception e) {
                Debug.echoError("Unable to disable '" + member.getClass().getName() + "'!");
                Debug.echoError(e);
            }
        }
    }
}
