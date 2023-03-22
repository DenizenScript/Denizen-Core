package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.Type;

public class CodeGenUtil {

    public static AsciiMatcher PERMITTED_NAME_CHARS = new AsciiMatcher(AsciiMatcher.LETTERS_LOWER + AsciiMatcher.LETTERS_UPPER + AsciiMatcher.DIGITS + "_");

    public static final String CORE_GEN_PACKAGE = "com/denizenscript/_generated_/";
    public static final String OBJECT_TAG_TYPE = Type.getInternalName(ObjectTag.class);
    public static final String OBJECT_LOCAL_TYPE = "L" + OBJECT_TAG_TYPE + ";";

    public static DynamicClassLoader loader = new DynamicClassLoader(CodeGenUtil.class.getClassLoader());

    public static String cleanName(String text) {
        String result = PERMITTED_NAME_CHARS.trimToMatches(text);
        if (result.length() > 50) {
            result = result.substring(0, 50);
        }
        return result;
    }

    public static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String className, byte[] bytecode) {
            Class<?> clazz = super.defineClass(className, bytecode, 0, bytecode.length);
            resolveClass(clazz);
            return clazz;
        }
        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return Class.forName(name);
            }
            catch (ClassNotFoundException ex) {
                Debug.echoError(ex);
                return super.findClass(name);
            }
        }
    }
}
