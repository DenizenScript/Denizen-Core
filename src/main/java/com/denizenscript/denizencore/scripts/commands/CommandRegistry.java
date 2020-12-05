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

    public static final DebugInvalidCommand debugInvalidCommand = new DebugInvalidCommand();

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
        // core
        registerCommand(AdjustCommand.class);
        registerCommand(DebugCommand.class);
        // Intentionally do not register the DebugInvalidCommand
        registerCommand(EventCommand.class);
        registerCommand(FlagCommand.class);
        registerCommand(ReloadCommand.class);
        registerCommand(SQLCommand.class);
        registerCommand(WebGetCommand.class);
        // file
        registerCommand(FileCopyCommand.class);
        registerCommand(LogCommand.class);
        registerCommand(YamlCommand.class);
        // queue
        registerCommand(ChooseCommand.class);
        registerCommand(DefineCommand.class);
        registerCommand(DetermineCommand.class);
        registerCommand(ElseCommand.class);
        registerCommand(ForeachCommand.class);
        registerCommand(GotoCommand.class);
        registerCommand(IfCommand.class);
        registerCommand(InjectCommand.class);
        registerCommand(MarkCommand.class);
        registerCommand(QueueCommand.class);
        registerCommand(RandomCommand.class);
        registerCommand(RateLimitCommand.class);
        registerCommand(RepeatCommand.class);
        registerCommand(RunCommand.class);
        registerCommand(StopCommand.class);
        registerCommand(WaitCommand.class);
        registerCommand(WaitUntilCommand.class);
        registerCommand(WhileCommand.class);
    }

    public <T extends AbstractCommand> void registerCommand(Class<T> cmd) {
        try {
            AbstractCommand command = cmd.newInstance();
            register(command.getName(), command);
        }
        catch (Throwable e) {
            Debug.echoError("Could not register command " + cmd.getName() + ", exception follows...");
            Debug.echoError(e);
        }
    }

    @Deprecated
    public <T extends AbstractCommand> void registerCoreMember(Class<T> cmd, String name, String hint, int args) {
        try {
            cmd.newInstance().as(name).withOptions(hint, args);
        }
        catch (Throwable e) {
            Debug.echoError("Could not register command " + name + ": " + e.getMessage());
            Debug.echoError(e);
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
