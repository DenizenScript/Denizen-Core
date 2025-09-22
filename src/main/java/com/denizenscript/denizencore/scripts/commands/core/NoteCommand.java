package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class NoteCommand extends AbstractCommand {

    public NoteCommand() {
        setName("note");
        setSyntax("note [<object>/remove] [as:<name>]");
        setRequiredArguments(2, 2);
        isProcedural = false;
        autoCompile();
        addRemappedPrefixes("as", "id", "i");
    }

    // <--[command]
    // @Name Note
    // @Syntax note [<object>/remove] [as:<name>]
    // @Required 2
    // @Maximum 2
    // @Short Adds or removes a named note of an object to the server.
    // @Synonyms Notable
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/advanced/notables.html
    //
    // @Description
    // Add or remove a 'note' to the server, persistently naming an object that can be referenced in events or scripts.
    // Only works for object types that are 'notable'.
    // Noted objects are "permanent" versions of other ObjectTags. (See: <@link language ObjectTags>)
    // Noted objects keep their properties when added.
    //
    // @Usage
    // Use to note a cuboid.
    // - note <[some_cuboid]> as:mycuboid
    //
    // @Usage
    // Use to remove a noted cuboid.
    // - note remove as:mycuboid
    //
    // @Usage
    // Use to note a location.
    // - note <context.location> as:mylocation
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("object") ObjectTag object,
                                   @ArgPrefixed @ArgName("as") String id) {
        boolean remove = object instanceof ElementTag && object.asElement().asLowerString().equals("remove");
        if (remove) {
            Notable note = NoteManager.getSavedObject(id);
            if (note != null) {
                note.forget();
                Debug.echoDebug(scriptEntry, "Note '" + id + "' removed");
            }
            else {
                Debug.echoDebug(scriptEntry, id + " is not saved");
            }
            return;
        }
        if (object instanceof ElementTag) {
            String stringified = object.toString();
            object = ObjectFetcher.pickObjectFor(stringified, scriptEntry.context);
            if (object == null) {
                Debug.echoError("Failed to read the object '" + stringified + "' into a real object value.");
                return;
            }
        }
        if (!(object instanceof Notable)) {
            Debug.echoError("Object '" + object + "' has type '" + object.getDenizenObjectType() + "' which is not a notable object type.");
            return;
        }
        try {
            ((Notable) object).makeUnique(id);
        }
        catch (Throwable ex) {
            Debug.echoError("Something went wrong converting that object!");
            Debug.echoError(ex);
        }
    }
}
