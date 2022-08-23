package com.denizenscript.denizencore.utilities.codegen;

import org.objectweb.asm.Type;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Helper for generating dynamic methods. */
public final class MethodGenerator {

    /** Generates a default empty object constructor impl. */
    public static void genDefaultConstructor(ClassWriter cw, String className) {
        MethodGenerator gen = generateMethod(className, cw, Opcodes.ACC_PUBLIC, "<init>", "()V");
        gen.loadThis();
        gen.invokeSpecial("java/lang/Object", "<init>", "()V");
        gen.returnNone();
        gen.end();
    }

    /** Generates a new method within the class. */
    public static MethodGenerator generateMethod(String className, ClassWriter cw, int access, String name, String descriptor) {
        MethodGenerator gen = new MethodGenerator(cw.visitMethod(access, name, descriptor, null, null), className, access);
        gen.start();
        return gen;
    }

    /** Constructs a new method generator. Prefer the static methods instead of this. */
    public MethodGenerator(MethodVisitor mv, String className, int access) {
        this.mv = mv;
        this.className = className;
        this.access = access;
    }

    /** Starts method generation. Do not call directly - this is automated. */
    public void start() {
        mv.visitCode();
        startLabel = new Label();
        advanceAndLabel(startLabel);
        if ((access & Opcodes.ACC_STATIC) == 0) {
            addLocal("this", "L" + className + ";");
        }
    }

    /** Ends the method generation. Must be called. */
    public void end() {
        for (Local local : locals) {
            finalizeLocalVar(local.name, local.descriptor, local.index);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /** The in-use classname. */
    public String className;

    /** The current method's access codes. */
    public int access;

    /** Underlying method bytecode generator. */
    public MethodVisitor mv;

    /** First label, automatically assigned and handled. */
    public Label startLabel;

    /** Last-used label. */
    public Label curLabel;

    /** Current line number. */
    public int lineNumber = 0;

    /** All current local variables. */
    public ArrayList<Local> locals = new ArrayList<>();

    /** Used to generate local variable indices properly. */
    public int nextLocalIndex = 0;

    /** Labels the upcoming section of code. */
    public void moveToLabel(Label label) {
        mv.visitLabel(label);
        curLabel = label;
    }

    /** Advances the line number by one. */
    public void advanceLineNumber() {
        mv.visitLineNumber(++lineNumber, curLabel);
    }

    /** Advances the line number by one and leaves a specific label. */
    public void advanceAndLabel(Label label) {
        moveToLabel(label);
        advanceLineNumber();
    }

    /** Advances the line number by one and leaves an arbitrary label. */
    public void advanceAndLabel() {
        advanceAndLabel(new Label());
    }

    /** Jumps to a given label directly, like a 'goto' statement. */
    public void jumpTo(Label label) {
        mv.visitJumpInsn(Opcodes.GOTO, label);
    }

    /** Jumps to a given label if the value on top of the stack is false (or 0). */
    public void jumpIfFalseTo(Label target) {
        mv.visitJumpInsn(Opcodes.IFEQ, target);
    }

    /** Jumps to a given label if the value on top of the stack is true (or non-0). */
    public void jumpIfTrueTo(Label target) {
        mv.visitJumpInsn(Opcodes.IFNE, target);
    }

    /** Jumps to a given label if the value on top of the stack is null. */
    public void jumpIfNullTo(Label target) {
        mv.visitJumpInsn(Opcodes.IFNULL, target);
    }

    /** Jumps to a given label if the value on top of the stack is not-null. */
    public void jumpIfNotNullTo(Label target) {
        mv.visitJumpInsn(Opcodes.IFNONNULL, target);
    }

    /** Casts the object on top of the stack to the given type. */
    public void cast(Class<?> type) {
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
    }

    /** Loads the given instance field from the object on top of the stack. */
    public void loadInstanceField(Field f) {
        loadInstanceField(Type.getInternalName(f.getDeclaringClass()), f.getName(), Type.getDescriptor(f.getType()));
    }

    /** Loads the given instance field from the object on top of the stack. */
    public void loadInstanceField(String className, String field, String descriptor) {
        mv.visitFieldInsn(Opcodes.GETFIELD, className, field, descriptor);
    }

    /** Loads the given static field. */
    public void loadStaticField(Field f) {
        loadStaticField(Type.getInternalName(f.getDeclaringClass()), f.getName(), Type.getDescriptor(f.getType()));
    }

    /** Loads the given static field. */
    public void loadStaticField(String className, String field, Class<?> type) {
        loadStaticField(className, field, Type.getDescriptor(type));
    }

    /** Loads the given static field. */
    public void loadStaticField(String className, String field, String descriptor) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, field, descriptor);
    }

    /** Internal usage - finalizes a local variable at the end. */
    public void finalizeLocalVar(String name, String descriptor, int index) {
        mv.visitLocalVariable(name, descriptor, null, startLabel, curLabel, index);
    }

    /** Invokes the given constructor. */
    public void invokeSpecial(Constructor<?> ctor) {
        invokeSpecial(Type.getInternalName(ctor.getDeclaringClass()), "<init>", Type.getConstructorDescriptor(ctor));
    }

    /** Invokes the given special method. */
    public void invokeSpecial(Method method) {
        invokeSpecial(Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method));
    }

    /** Invokes the given special method. */
    public void invokeSpecial(String type, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, type, methodName, descriptor, false);
    }

    /** Invokes the given virtual object method. */
    public void invokeVirtual(Method method) {
        invokeVirtual(Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method));
    }

    /** Invokes the given virtual object method. */
    public void invokeVirtual(String type, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, type, methodName, descriptor, false);
    }

    /** Invokes the given static method. */
    public void invokeStatic(Method method) {
        invokeStatic(Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method));
    }

    /** Invokes the given static method. */
    public void invokeStatic(String type, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, type, methodName, descriptor, false);
    }

    /** Invokes the given interface method. */
    public void invokeInterface(Method method) {
        invokeInterface(Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method));
    }

    /** Invokes the given interface method. */
    public void invokeInterface(String type, String methodName, String descriptor) {
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, type, methodName, descriptor, true);
    }

    /** Loads a raw string directly into the stack. */
    public void loadString(String text) {
        mv.visitLdcInsn(text);
    }

    /** Loads a raw (short) integer directly into the stack. */
    public void loadInt(int intVal) {
        mv.visitIntInsn(Opcodes.SIPUSH, intVal);
    }

    /** Duplicates the value on top of the stack. */
    public void stackDuplicate() {
        mv.visitInsn(Opcodes.DUP);
    }

    /** Creates an object array of the given type. */
    public void createArray(Class<?> type) {
        mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(type));
    }

    /** Stores a value into an array on the stack of the given type. */
    public void arrayStore(Class<?> arrayType) {
        arrayStore(Type.getDescriptor(arrayType));
    }

    /** Stores a value into an array on the stack of the given type descriptor. */
    public void arrayStore(String arrayTypeDescriptor) {
        mv.visitInsn(getOpcodeTypeOffset(Opcodes.IASTORE, arrayTypeDescriptor));
    }

    /** Loads the 'this' local. */
    public void loadThis() {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
    }

    /** Loads a value from the local variable. */
    public void loadLocal(Local local) {
        mv.visitVarInsn(getOpcodeTypeOffset(Opcodes.ILOAD, local.descriptor), local.index);
    }

    /** Stores a value into the local variable. */
    public void storeLocal(Local local) {
        mv.visitVarInsn(getOpcodeTypeOffset(Opcodes.ISTORE, local.descriptor), local.index);
    }

    /** Generates a code equivalent to "return;" with no value. */
    public void returnNone() {
        mv.visitInsn(Opcodes.RETURN);
    }

    /** Generates a return code appropriate for the given type. Accepts 'void.class' as return none. */
    public void returnValue(Class<?> type) {
        returnValue(Type.getDescriptor(type));
    }

    /** Generates a return code appropriate for the given type. Accepts "V" as return none. */
    public void returnValue(String valTypeDescriptor) {
        if (valTypeDescriptor.equals("V")) {
            returnNone();
        }
        else {
            mv.visitInsn(getOpcodeTypeOffset(Opcodes.IRETURN, valTypeDescriptor));
        }
    }

    /** Adds a local variable of the given name and type. Assigns index automatically. */
    public Local addLocal(String name, Class type) {
        return addLocal(name, Type.getDescriptor(type));
    }

    /** Adds a local variable of the given name and type descriptor. Assigns index automatically. */
    public Local addLocal(String name, String descriptor) {
        int index = nextLocalIndex++;
        char c = descriptor.charAt(0);
        if (c == 'D' || c == 'J') {
            nextLocalIndex++; // Doubles and longs have two vars secretly for some reason
        }
        return addLocal(name, descriptor, index);
    }

    /** Adds a local variable of the given name and type descriptor, with a manually selected inex. */
    public Local addLocal(String name, String descriptor, int index) {
        Local local = new Local();
        local.name = name;
        local.descriptor = descriptor;
        local.index = index;
        locals.add(local);
        return local;
    }

    /** Represents a local variable. */
    public static class Local {

        public String name;

        public int index;

        public String descriptor;
    }

    /** Internal usage. */
    public static int getOpcodeTypeOffset(int iOpcode, String typeDescriptor) {
        // I, L, F, D, A
        switch (typeDescriptor.charAt(0)) {
            case 'L': return iOpcode + 4;
            case 'I': case 'Z': case 'B': case 'C': case 'S': return iOpcode; // Everything sub-integer-width is secretly integers
            case 'F': return iOpcode + 2;
            case 'D': return iOpcode + 3;
            case 'J': return iOpcode + 1;
            default:
                throw new DenizenCodeGenException("Invalid type '" + typeDescriptor + "' cannot be used.");
        }
    }
}
