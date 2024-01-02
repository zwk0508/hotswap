package com.zwk;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/1/2 10:55
 */

public class MyClassVisitor extends ClassVisitor {
    String className;
    HotSwapMatcher matcher;

    public MyClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        ServiceLoader<HotSwapMatcher> serviceLoader = ServiceLoader.load(HotSwapMatcher.class);
        Iterator<HotSwapMatcher> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            matcher = iterator.next();
        } else {
            matcher = HotSwapMatcher.DEFAULT_MATCHER;
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("<init>".equals(name) || "<clinit>".equals(name) || Modifier.isAbstract(access)) {
            return methodVisitor;
        }
        if (matcher instanceof MethodMatcher && !matcher.match(className, access, name, descriptor)) {
            return methodVisitor;
        }
        return new MyMethodVisitor(Opcodes.ASM9, methodVisitor, className, name, access, descriptor, matcher);
    }
}
