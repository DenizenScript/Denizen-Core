package net.aufdemrand.denizencore.scripts.commands;

import net.aufdemrand.denizencore.interfaces.RegistrationableInstance;
import net.aufdemrand.denizencore.interfaces.dRegistry;
import net.aufdemrand.denizencore.scripts.commands.core.IfCommand;
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
        this.instances.put(commandName.toUpperCase(), (AbstractCommand) commandInstance);
        this.classes.put(((AbstractCommand) commandInstance).getClass(), commandName.toUpperCase());
        return true;
    }

    @Override
    public Map<String, AbstractCommand> list() {
        return instances;
    }

    @Override
    public AbstractCommand get(String commandName) {
        return instances.get(commandName.toUpperCase());
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
        registerCoreMember(IfCommand.class, "NEOIF", "Compares values and executes a code block.", 0);
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
