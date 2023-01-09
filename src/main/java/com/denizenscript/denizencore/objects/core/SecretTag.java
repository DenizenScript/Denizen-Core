package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ReflectionRefuse;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.io.File;

@ReflectionRefuse
public class SecretTag implements ObjectTag {

    @ReflectionRefuse
    public static YamlConfiguration secretsFile;

    public static void load() {
        File path = new File(DenizenCore.implementation.getDataFolder(), "secrets.secret");
        String fileContent = CoreUtilities.journallingLoadFile(path.getPath());
        if (fileContent != null) {
            if (!fileContent.startsWith("!SECRETS_FILE")) {
                Debug.echoError("Secrets file (Denizen/secrets.secret) is not properly formatted. Refusing to load. Please delete and reset it.");
                return;
            }
            fileContent = fileContent.substring("!SECRETS_FILE".length());
            secretsFile = YamlConfiguration.load(fileContent);
        }
        else {
            secretsFile = new YamlConfiguration();
            CoreUtilities.journallingFileSave(path.getPath(), "!SECRETS_FILE\n# Denizen Secrets File\n# Refer to meta documentation for SecretTag Objects: https://meta.denizenscript.com/Docs/ObjectTypes/SecretTag"
                    + "\n\n# my_token: abc123.123abc\n");
        }
    }

    // <--[ObjectType]
    // @name SecretTag
    // @prefix secret
    // @base ElementTag
    // @ExampleTagBase secret[my_secret_key]
    // @ExampleValues <secret[my_secret_key]>
    // @ExampleForReturns
    // - webget %VALUE% "post:Message to secret address!"
    // @format
    // The identity format for secrets is simply the secret key (as defined by the file 'secrets.secret' in the Denizen folder).
    //
    // @description
    // A SecretTag represents a value that should never be exposed to logs or tags.
    // For example: authorization tokens, API keys, etc.
    //
    // A SecretTag is made of a 'key', and a 'value'.
    // The key is a simple name, like 'my_bot_token', that is safe to show in logs/etc.
    // The value is the actual internal data that must be kept secret, often a generated code.
    //
    // The keys and values must be defined in the 'secrets.secret' file inside the Denizen folder.
    // The contents of that file would look something like:
    // !SECRETS_FILE
    // my_bot_token: abc123.123abc
    // my_api_key: 1a2b3c4d5e6f
    //
    // The above example defines SecretTag 'my_bot_token' as 'abc123.123abc',
    // meaning you could then use '<secret[my_bot_token]>' in the input to a command that parses secrets to have it understand the real value to input should be 'abc123.123abc'
    // However if you use the same tag in for example a narrate command, it would just narrate 'secret@my_bot_token', keeping your real value safe.
    // Note that the "!SECRETS_FILE" prefix cannot be removed, but comments can be added/removed/altered freely with a "#" prefix.
    //
    // Commands that accept SecretTag inputs will document that information in the command meta. For example, see <@link command webget>.
    //
    // There is intentionally no tag that can read the value of a secret.
    //
    // You can reload the secrets file via "/ex reload config"
    //
    // -->

    @Fetchable("secret")
    public static SecretTag valueOf(String string, TagContext context) {
        if (string.startsWith("secret@")) {
            string = string.substring("secret@".length());
        }
        SecretTag secret = new SecretTag(string);
        // Make sure it's valid.
        if (secret.isValid()) {
            return secret;
        }
        else {
            return null;
        }
    }

    public static boolean matches(String string) {
        if (CoreUtilities.toLowerCase(string).startsWith("secret@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    public SecretTag(String key) {
        this.key = key;
    }

    public String key;

    private String prefix = "Secret";

    public boolean isValid() {
        return getValue() != null;
    }

    @ReflectionRefuse
    public String getValue() {
        if (secretsFile == null) {
            return null;
        }
        return secretsFile.getString(key);
    }

    @Override
    public String identify() {
        return "secret@" + key;
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
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        return "<LG>secret@<Y>" + key;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    public static void register() {

        // <--[tag]
        // @attribute <SecretTag.key>
        // @returns ElementTag
        // @description
        // Returns the secret key for this secret object.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "key", (attribute, object) -> {
            return new ElementTag(object.key, true);
        });
    }

    public static ObjectTagProcessor<SecretTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }
}
