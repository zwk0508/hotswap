package com.zwk;

import org.objectweb.asm.*;

import java.lang.reflect.Modifier;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/1/2 11:04
 */

public class MyMethodVisitor extends MethodVisitor {

    boolean hotswap = true;
    String className;
    String methodName;
    int access;
    String descriptor;
    HotSwapMatcher matcher;
    Label retLabel = new Label();
    Label preLabel = new Label();

    public MyMethodVisitor(int api, MethodVisitor methodVisitor, String className, String methodName, int access, String descriptor, HotSwapMatcher matcher) {
        super(api, methodVisitor);
        this.className = className;
        this.methodName = methodName;
        this.access = access;
        this.descriptor = descriptor;
        this.matcher = matcher;
        if (matcher instanceof AnnotationMatcher) {
            hotswap = false;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (!hotswap && matcher.match(descriptor)) {
            hotswap = true;
        }
        return super.visitAnnotation(descriptor, visible);
    }


    @Override
    public void visitCode() {
        super.visitCode();
        if (hotswap) {
            visitLdcInsn(className + "/" + methodName + ".groovy");
            visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/zwk/processor/ProcessorHelper",
                    "getProcessor",
                    "(Ljava/lang/String;)Lcom/zwk/processor/Processor;",
                    false);
            Type[] argumentTypes = Type.getArgumentTypes(descriptor);

            boolean isStatic = Modifier.isStatic(access);

            int storeLocation = getStoreLocation(argumentTypes, isStatic);

            visitVarInsn(Opcodes.ASTORE, storeLocation);
            visitVarInsn(Opcodes.ALOAD, storeLocation);
            visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    "com/zwk/processor/Processor",
                    "isUpdate",
                    "()Z",
                    true);
            visitJumpInsn(Opcodes.IFEQ, preLabel);

            visitVarInsn(Opcodes.ALOAD, storeLocation);

            if (isStatic) {
                visitLdcInsn(className.replace('/', '.'));
                visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            } else {
                visitVarInsn(Opcodes.ALOAD, 0);
            }
            int argLen = argumentTypes.length;

            visitIntInsn(Opcodes.BIPUSH, argLen);
            visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

            int location = 0;
            for (int i = 0; i < argLen; i++) {
                Type argumentType = argumentTypes[i];
                visitInsn(Opcodes.DUP);
                visitIntInsn(Opcodes.BIPUSH, i);
                if (argumentType == Type.BOOLEAN_TYPE) {
                    visitVarInsn(Opcodes.ILOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                    location++;
                } else if (argumentType == Type.BYTE_TYPE) {
                    visitVarInsn(Opcodes.ILOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                    location++;
                } else if (argumentType == Type.SHORT_TYPE) {
                    visitVarInsn(Opcodes.ILOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                    location++;
                } else if (argumentType == Type.INT_TYPE) {
                    visitVarInsn(Opcodes.ILOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                    location++;
                } else if (argumentType == Type.LONG_TYPE) {
                    visitVarInsn(Opcodes.LLOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    location += 2;
                } else if (argumentType == Type.FLOAT_TYPE) {
                    visitVarInsn(Opcodes.FLOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                    location++;
                } else if (argumentType == Type.DOUBLE_TYPE) {
                    visitVarInsn(Opcodes.DLOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    location += 2;
                } else if (argumentType == Type.CHAR_TYPE) {
                    visitVarInsn(Opcodes.ILOAD, location);
                    visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                    location++;
                } else {
                    visitVarInsn(Opcodes.ALOAD, location);
                    location++;
                }
                visitInsn(Opcodes.AASTORE);
            }
            visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    "com/zwk/processor/Processor",
                    "process",
                    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                    true);

            Type returnType = Type.getReturnType(descriptor);
            if (returnType == Type.VOID_TYPE) {
                visitInsn(Opcodes.POP);
            } else if (returnType == Type.BOOLEAN_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Boolean.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            } else if (returnType == Type.BYTE_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Byte.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
            } else if (returnType == Type.SHORT_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Short.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
            } else if (returnType == Type.INT_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            } else if (returnType == Type.LONG_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Long.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
            } else if (returnType == Type.FLOAT_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Float.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
            } else if (returnType == Type.DOUBLE_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Double.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            } else if (returnType == Type.CHAR_TYPE) {
                visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Character.class));
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
            } else {
                visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
            }
            visitJumpInsn(Opcodes.GOTO, retLabel);
            visitLabel(preLabel);
        }
    }

    private int getStoreLocation(Type[] argumentTypes, boolean isStatic) {
        int location = isStatic ? 0 : 1;
        for (Type argumentType : argumentTypes) {
            if (argumentType == Type.DOUBLE_TYPE || argumentType == Type.LONG_TYPE) {
                location += 2;
            } else {
                location++;
            }
        }
        return location;
    }


    @Override
    public void visitInsn(int opcode) {
        if (opcode >= Opcodes.IRETURN
                && opcode <= Opcodes.RETURN) {
            if (hotswap) {
                visitLabel(retLabel);
            }
        }
        super.visitInsn(opcode);
    }
}
