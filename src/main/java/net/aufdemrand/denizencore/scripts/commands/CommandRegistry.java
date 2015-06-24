package net.aufdemrand.denizencore.scripts.commands;

import net.aufdemrand.denizencore.interfaces.RegistrationableInstance;
import net.aufdemrand.denizencore.interfaces.dRegistry;
import net.aufdemrand.denizencore.scripts.commands.core.*;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.HashMap;
import java.util.Map;

public abstract class CommandRegistry implements dRegistry {
    public CommandRegistry() {
    }

    public final Map<String, AbstractCommand> instances = new HashMap<String, AbstractCommand>();
    public final Map<Class<? extends AbstractCommand>, String> classes = new HashMap<Class<? extends AbstractCommand>, String>();

    @Override
    public boolean register(String commandName, RegistrationableInstance commandInstance) {
        this.instances.put(CoreUtilities.toLowerCase(commandName), (AbstractCommand) commandInstance);
        this.classes.put(((AbstractCommand) commandInstance).getClass(), commandName.toUpperCase());
        return true;
    }

    @Override
    public Map<String, AbstractCommand> list() {
        return instances;
    }

    @Override
    public AbstractCommand get(String commandName) {
        return instances.get(CoreUtilities.toLowerCase(commandName));
    }

    @Override
    public <T extends RegistrationableInstance> T get(Class<T> clazz) {
        String command = classes.get(clazz);
        if (command != null) return clazz.cast(instances.get(command));
        else return null;
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
    //
    // A few examples:
    // [<location>] is required and non-literal... you might fill it with 'l@1,2,3,world' which is a valid location object.
    // (sound:{true}/false) is optional and has a default value of true... you can put sound:false to prevent sound, or leave it blank to allow sound.
    // (repeats:<#>) is optional, has no clear default, and is a number. You can put repeats:3 to repeat three times, or leave it blank to not repeat.
    // Note: Optional arguments without a default usually have a secret default... EG, the (repeats:<#>) above has a secret default of '0'.
    //
    // Also, you should never directly type in [], (), {}, or <> even though they are in the syntax info.
    // The only exception is in a replaceable tag (EG: <npc.has_trait[<traitname>]> will take <npc.has_trait[mytrait]> as a valid actual usage)
    //
    // Highly specific note: <commands> means a block of commands wrapped in braces... EG:
    // <code>
    // - repeat 3 {
    //   - narrate "%value%"
    //   - narrate "everything between the {and} symbols (including them) are for the <commands> input!"
    //   }
    // </code>
    //
    // -->

    public void registerCoreCommands() {

        // <--[command]
        // @Name Debug
        // @Syntax debug [<type>] [<message>] (name:<name>)
        // @Required 2
        // @Stable stable
        // @Short Shows a debug message.
        // @Author mcmonkey
        // @Group core
        // @Description
        // Use to quickly output debug information to console.
        // Valid types include:
        // DEBUG: standard hideable debug.
        // HEADER: standard hideable debug inside a header line.
        // FOOTER: a footer line.
        // SPACER: a spacer line.
        // LOG: global output, non-hideable.
        // APPROVAL: "Okay!" output, non-hideable.
        // ERROR: "Error!" output, non-hideable.
        // REPORT: normally used to describe the arguments of a command, requires a name, hideable.
        // EXCEPTION: outputs a full java stacktrace.
        // @Tags
        // None
        // @Usage
        // Use to show an error
        // - debug error "Something went wrong!"
        // @Usage
        // Use to add some information to help your own ability to read debug output from you script
        // - debug debug "Time is currently <def[milliseconds].div[1000].round> seconds!"
        // -->
        registerCoreMember(DebugCommand.class,
                "debug", "debug [<type>] [<message>] (name:<name>)", 2);


        // <--[command]
        // @Name Define
        // @Syntax define [<id>] [<value>]
        // @Required 1
        // @Stable stable
        // @Short Creates a temporary variable inside a script queue.
        // @Author aufdemrand
        // @Group core

        // @Description
        // Definitions are queue-level (or script-level) 'variables' that can be used throughout a script, once
        // defined, by using %'s around the definition id/name. Definitions are only valid on the current queue and are
        // not transferred to any new queues constructed within the script, such as a 'run' command, without explicitly
        // specifying to do so.
        //
        // Definitions are lighter and faster than creating a temporary flag, but unlike flags, are only a single entry,
        // that is, you can't add or remove from the definition, but you can re-create it if you wish to specify a new
        // value. Definitions are also automatically destroyed when the queue is completed, so there is no worry for
        // leaving unused data hanging around.
        //
        // Definitions are also resolved before replaceable tags, meaning you can use them within tags, even as an
        // attribute. ie. <%player%.name>

        // @Tags
        // %<ID>% to get the value assign to an ID
        // <def[<ID>]> to get the value assigned to an ID

        // @Usage
        // Use to make complex tags look less complex, and scripts more readable
        // - narrate 'You invoke your power of notice...'
        // - define range '<player.flag[range_level].mul[3]>'
        // - define blocks '<player.flag[noticeable_blocks]>'
        // - narrate '[NOTICE] You have noticed <player.location.find.blocks[<def[blocks]>].within[<def[range]>].size>
        // blocks in the area that may be of interest.'

        // @Usage
        // Use to keep the value of a replaceable tag that you might use many times within a single script. Definitions
        // can be faster and cleaner than reusing a replaceable tag over and over
        // - define arg1 <c.args.get[1]>
        // - if <def[arg1]> == hello narrate 'Hello!'
        // - if <def[arg1]> == goodbye narrate 'Goodbye!'

        // @Usage
        // Use to pass some important information (arguments) on to another queue
        // - run 'new_task' d:hello|world
        // 'new_task' now has some definitions, <def[1]> and <def[2]>, that contains the contents specified, 'hello' and 'world'.

        // @Usage
        // Use to remove a definition
        // - define myDef:!

        // -->
        registerCoreMember(DefineCommand.class,
                "define", "define [<id>] [<value>]", 1);


        // <--[command]
        // @Name Determine
        // @Syntax determine (passively) [<value>]
        // @Required 1
        // @Stable stable
        // @Short Sets the outcome of a world event.
        // @Author aufdemrand
        // @Group core
        // @Description
        // TODO: Document Command Details
        // @Tags
        // TODO: Document Command Details
        // @Usage
        // Use to modify the result of an event
        // - determine <context.message.substring[5]>
        // @Usage
        // Use to cancel an event, but continue running script commands
        // - determine passively cancelled
        // -->
        registerCoreMember(DetermineCommand.class,
                "determine", "determine (passively) [<value>]", 1);


        // <--[command]
        // @Name Foreach
        // @Syntax foreach [stop/next/<object>|...] [<commands>]
        // @Required 1
        // @Stable stable
        // @Short Loops through a dList, running a set of commands for each item.
        // @Author Morphan1, mcmonkey
        // @Group core
        // @Video /denizen/vids/Loops

        // @Description
        // Loops through a dList of any type. For each item in the dList, the specified commands will be ran for
        // that list entry. To call the value of the entry while in the loop, you can use <def[value]>.
        //
        // To end a foreach loop, do - foreach stop
        //
        // To jump immediately to the next entry in the loop, do - foreach next

        // @Tags
        // <def[value]> to get the current item in the loop
        // <def[loop_index]> to get the current loop iteration number

        // @Usage
        // Use to run commands for 'each entry' in a list of objects/elements.
        // - foreach li@e@123|n@424|p@BobBarker {
        //     - announce "There's something at <def[value].location>!"
        //   }

        // @Usage
        // Use to iterate through entries in any tag that returns a list
        // - foreach <server.list_online_players> {
        //     - narrate "Thanks for coming to our server! Here's a bonus $50.00!"
        //     - give <def[value]> money qty:50
        //   }

        // -->
        registerCoreMember(ForeachCommand.class,
                "foreach", "foreach [stop/next/<object>|...] [<commands>]", 1);


        // <--[command]
        // @Name Goto
        // @Syntax goto [<name>]
        // @Required 1
        // @Stable stable
        // @Short Jump forward to a location marked by <@link command mark>.
        // @Author mcmonkey
        // @Group core
        // @Description
        // Jumps forward to a marked location in the script.
        // For example:
        // <code>
        // - goto potato
        // - narrate "This will never show"
        // - mark potato
        // </code>
        // @Tags
        // None
        // @Usage
        // Use to jump forward to a location.
        // - goto potato
        // -->
        registerCoreMember(GotoCommand.class, "GOTO", "goto [<name>]", 1);


        // <--[command]
        // @Name If
        // @Syntax if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>] (else <commands>)
        // @Required 1
        // @Stable stable
        // @Short Compares values, and runs one script if they match, or a different script if they don't match.
        // @Author aufdemrand, David Cernat
        // @Group core
        // @Video /denizen/vids/Alternate/Dynamic%20Actions:%20The%20If%20Command
        // @Description
        // TODO: Document Command Details
        // @Tags
        // <el@element.is[<operator>].to[<element>]>
        // @Usage
        // TODO: Document Command Details
        // -->
        registerCoreMember(IfCommand.class, "IF", "if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>] (else <commands>)", 1);


        // <--[command]
        // @Name Mark
        // @Syntax mark [<name>]
        // @Required 1
        // @Stable stable
        // @Short Marks a location for <@link command goto>.
        // @Author mcmonkey
        // @Group core
        // @Description
        // Marks a location for the goto command. See <@link command goto> for details.
        // @Tags
        // None
        // @Usage
        // Use to mark a location.
        // - mark potato
        // -->
        registerCoreMember(MarkCommand.class, "MARK", "mark [<name>]", 1);
    }

    public <T extends AbstractCommand> void registerCoreMember(Class<T> cmd, String names, String hint, int args) {
        for (String name : names.split(", ")) {

            try {
                cmd.newInstance().activate().as(name).withOptions(hint, args);
            } catch(Throwable e) {
                dB.echoError("Could not register command " + name + ": " + e.getMessage());
                dB.echoError(e);
            }
        }
    }

    @Override
    public void disableCoreMembers() {
        for (RegistrationableInstance member : instances.values())
            try {
                member.onDisable();
            } catch (Exception e) {
                dB.echoError("Unable to disable '" + member.getClass().getName() + "'!");
                dB.echoError(e);
            }
    }
}
