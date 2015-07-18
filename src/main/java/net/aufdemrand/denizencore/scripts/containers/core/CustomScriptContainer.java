package net.aufdemrand.denizencore.scripts.containers.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.interfaces.ContextSource;
import net.aufdemrand.denizencore.objects.CustomObject;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.text.StringHolder;

import java.util.HashMap;
import java.util.List;

public class CustomScriptContainer extends ScriptContainer {

    public HashMap<String, String> defaultVars = new HashMap<String, String>();

    public HashMap<String, String> getVars() {
        HashMap<String, String> vars;
        if (inherit != null) {
            ScriptContainer sc = ScriptRegistry.getScriptContainer(inherit);
            if (sc != null && sc instanceof CustomScriptContainer) {
                vars = ((CustomScriptContainer) sc).getVars();
            }
            else {
                vars = new HashMap<String, String>();
            }
        }
        else {
            vars = new HashMap<String, String>();
        }
        for (String str : defaultVars.keySet()) {
            vars.remove(str);
            vars.put(str, defaultVars.get(str));
        }
        return vars;
    }

    public String inherit = null;

    public CustomScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);

        for (StringHolder str : getConfigurationSection("").getKeys(false)) {
            if (str.low.equals("inherit")) {
                inherit = getString(str.str);
            }
            else if (!(str.low.equals("type") || str.low.equals("tags") || str.low.equals("mechanisms"))) {
                defaultVars.put(str.low, getString(str.str));
            }
        }
    }

    public boolean hasPath(String path) {
        CustomScriptContainer csc = this;
        while (csc != null) {
            if (csc.contains(path)) {
                return true;
            }
            csc = ScriptRegistry.getScriptContainerAs(csc.inherit, CustomScriptContainer.class);
        }
        return false;
    }

    public long runTagScript(String path, CustomObject obj, ScriptEntryData data) {
        CustomScriptContainer csc = this;
        while (csc != null) {
            if (csc.contains("tags." + path)) {
                dB.echoDebug(this, "[CustomObject] Calculating tag: " + path + " for " + csc.getName());
                ScriptQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId("TAG_" + csc.getName() + "_" + path + "__"));
                List<ScriptEntry> listOfEntries = csc.getEntries(data, "tags." + path);
                long id = DetermineCommand.getNewId();
                ScriptBuilder.addObjectToEntries(listOfEntries, "ReqId", id);
                CustomScriptContextSource cscs = new CustomScriptContextSource();
                cscs.obj = obj;
                queue.setContextSource(cscs);
                queue.addEntries(listOfEntries);
                queue.start();
                return id;
            }
            dB.echoDebug(this, "[CustomObject] Grabbing parent of " + csc.getName());
            csc = ScriptRegistry.getScriptContainerAs(csc.inherit, CustomScriptContainer.class);
        }
        dB.echoDebug(this, "Unable to find tag handler for " + path + " for " + this.getName());
        return -1;
    }

    public long runMechScript(String path, CustomObject obj, String value) {
        CustomScriptContainer csc = this;
        while (csc != null) {
            if (csc.contains("mechanisms." + path)) {
                ScriptQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId("MECH_" + csc.getName() + "_" + path + "__"));
                List<ScriptEntry> listOfEntries = csc.getEntries(DenizenCore.getImplementation().getEmptyScriptEntryData(), "mechanisms." + path);
                long id = DetermineCommand.getNewId();
                ScriptBuilder.addObjectToEntries(listOfEntries, "ReqId", id);
                CustomScriptContextSource cscs = new CustomScriptContextSource();
                cscs.obj = obj;
                cscs.value = new Element(value);
                queue.setContextSource(cscs);
                queue.addEntries(listOfEntries);
                queue.start();
                return id;
            }
            csc = ScriptRegistry.getScriptContainerAs(csc.inherit, CustomScriptContainer.class);
        }
        return -1;
    }

    public static class CustomScriptContextSource implements ContextSource {

        public CustomObject obj;

        public Element value;

        @Override
        public boolean getShouldCache() {
            return true;
        }

        @Override
        public dObject getContext(String name) {
            if (name.equals("this")) {
                return obj;
            }
            else if (name.equals("value")) {
                 return value;
            }
            else {
                 return null;
            }
        }
    }
}
