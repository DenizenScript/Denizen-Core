package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.containers.core.CustomScriptContainer;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class CustomObject implements dObject, Adjustable {

    @Fetchable("custom")
    public static CustomObject valueOf(String string, TagContext context) {
        Matcher m;

        ///////
        // Handle objects with properties through the object fetcher
        m = ObjectFetcher.DESCRIBED_PATTERN.matcher(string);
        if (m.matches()) {
            return ObjectFetcher.getObjectFrom(CustomObject.class, string, context);
        }

        if (string.startsWith("custom@")) {
            string = string.substring("custom@".length());
        }

        String typeData = string;
        ScriptContainer sc = ScriptRegistry.getScriptContainer(typeData);
        if (sc == null) {
            if (context == null || context.debug) {
                dB.echoError("Null script container for " + typeData);
            }
            return null;
        }
        if (!(sc instanceof CustomScriptContainer)) {
            if (context == null || context.debug) {
                dB.echoError("Wrong-typed script container for " + typeData);
            }
            return null;
        }
        return new CustomObject((CustomScriptContainer) sc, ((CustomScriptContainer) sc).getVars());
    }

    public static boolean matches(String string) {
        return string.startsWith("custom@");
    }

    public CustomScriptContainer container;
    public Map<String, String> vars;

    public CustomObject(CustomScriptContainer type, Map<String, String> values) {
        container = type;
        vars = values;
    }

    private String prefix = "Custom";

    @Override
    public String getObjectType() {
        return "Custom";
    }

    @Override
    public String identify() {
        StringBuilder outp = new StringBuilder();
        for (Map.Entry<String, String> var : vars.entrySet()) {
            outp.append(var.getKey() + "=" + var.getValue().replace(';', (char) 0x2011) + ";");
        }
        return "custom@" + container.getName() + "[" + (outp.length() > 0 ? outp.substring(0, outp.length() - 1) : "") + "]";
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public dObject setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debug() {
        return String.format("<G>%s='<A>%s<G>'  ", prefix, identify());
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String getAttribute(Attribute attribute) {
        if (attribute == null) {
            return "null";
        }

        String res = vars.get(attribute.getAttribute(1));
        if (res == null) {
            if (container.hasPath("tags." + attribute.getAttribute(1))) {
                long ID = container.runTagScript(attribute.getAttribute(1), this,
                        attribute.getScriptEntry() != null ? attribute.getScriptEntry().entryData : null);
                List<String> outcomes = DetermineCommand.getOutcome(ID);
                if (outcomes == null) {
                    return null;
                }
                return ObjectFetcher.pickObjectFor(outcomes.get(0)).getAttribute(attribute.fulfill(1));
            }
            return new Element(identify()).getAttribute(attribute);
        }
        return ObjectFetcher.pickObjectFor(res).getAttribute(attribute.fulfill(1));
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        adjust(mechanism);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        String name = CoreUtilities.toLowerCase(mechanism.getName());
        String value = mechanism.getValue().asString();
        if (container.hasPath("mechanisms." + name)) {
            long ID = container.runMechScript(name, this, value);
            List<String> outcomes = DetermineCommand.getOutcome(ID);
            if (outcomes == null) {
                return;
            }
            CustomObject co = CustomObject.valueOf(outcomes.get(0), null);
            container = co.container;
            vars = co.vars;
        }
        else {
            vars.remove(name);
            vars.put(name, value);
        }
    }
}
