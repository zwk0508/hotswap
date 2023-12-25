package com.zwk;

import com.zwk.annotation.HotSwap;
import javassist.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/12/22 13:50
 */

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            System.err.println("usage: java -jar main.jar /path/file");
            return;
        }

        String f = args[0];
        if (f.endsWith(".jar")) {
            processJarFile(f);
        } else if (f.endsWith(".class")) {
            processFile(f);
        } else if (new File(f).isDirectory()) {
            processDir(f);
        } else {
            System.err.println("unknown file,can be class file jar file or directory");
        }

    }


    private static void processDir(String dir) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        File file = new File(dir);
        processDir(file, pool);
    }

    private static void processDir(File file, ClassPool pool) throws Exception {
        if (!file.isDirectory()) {
            if (!file.getName().endsWith(".class")) {
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            CtClass ctClass = pool.makeClass(new ByteArrayInputStream(bytes));
            processMethods(ctClass);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(ctClass.toBytecode());
            }
        } else {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    processDir(file1, pool);
                }
            }
        }


    }


    private static void processJarFile(String jarFileName) throws Exception {
        File file = new File(jarFileName);
        JarFile jarFile = new JarFile(file);
        ClassPool pool = ClassPool.getDefault();
        Enumeration<JarEntry> entries = jarFile.entries();
        Map<String, byte[]> map = new HashMap<>();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                CtClass ctClass = pool.makeClass(inputStream);
                processMethods(ctClass);
                map.put(jarEntry.getName(), ctClass.toBytecode());

            }
        }
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file.getName()))) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                byte[] bytes = map.get(name);
                if (bytes == null) {
                    jarOutputStream.putNextEntry(jarEntry);
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    byte[] reads = new byte[8192];
                    int length;
                    while ((length = inputStream.read(reads)) > 0) {
                        jarOutputStream.write(reads, 0, length);
                    }
                } else {
                    JarEntry jarEntry1 = new JarEntry(jarEntry.getName());
                    jarEntry1.setSize(bytes.length);
                    jarOutputStream.putNextEntry(jarEntry1);
                    jarOutputStream.write(bytes);
                }
            }
        }
    }

    private static void processMethods(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
        for (CtMethod declaredMethod : declaredMethods) {
            if (declaredMethod.hasAnnotation(HotSwap.class)) {

                String longName = declaredMethod.getLongName();

                String s = fortmatBody(
                        longName,
                        declaredMethod.getParameterTypes(),
                        declaredMethod.getReturnType());
                declaredMethod.insertBefore(s);
            }
        }
    }

    private static void processFile(String fileName) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.makeClass(new FileInputStream(fileName));
        processMethods(ctClass);
        byte[] bytes = ctClass.toBytecode();
        String className = ctClass.getName();
        className = className.replace('.', File.separatorChar);
        File file = new File(className + ".class");
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
        }

    }


    private static String fortmatBody(String key, CtClass[] params, CtClass retType) {
        StringBuilder sb = new StringBuilder();
        int length = params.length;
        sb.append("com.zwk.processor.Processor processor = com.zwk.processor.ProcessorHelper.getProcessor(\"")
                .append(key)
                .append("\");")
                .append("boolean update = processor.isUpdate(); if (update) {")
                .append("Object thiz = $0;")
                .append("Object [] args = new Object[")
                .append(length)
                .append("];");
        if (length > 0) {
            for (int i = 0; i < length; i++) {
                CtClass param = params[i];
                sb.append("args[")
                        .append(i)
                        .append("] = ")
                        .append(getParam(param, i))
                        .append(";");
            }
        }
        sb.append("Object ret = processor.process(");
        sb.append("thiz,args);");
        if (retType != CtClass.voidType) {
            sb.append("return ")
                    .append(getReturnValue(retType))
                    .append(";");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String getReturnValue(CtClass retType) {
        if (retType == CtClass.booleanType) {
            return "((Boolean)ret).booleanValue()";
        }
        if (retType == CtClass.byteType) {
            return "((Byte)ret).byteValue()";
        }
        if (retType == CtClass.shortType) {
            return "((Short)ret).shortValue()";
        }
        if (retType == CtClass.intType) {
            return "((Integer)ret).intValue()";
        }
        if (retType == CtClass.longType) {
            return "((Long)ret).longValue()";
        }
        if (retType == CtClass.floatType) {
            return "((Float)ret).floatValue()";
        }
        if (retType == CtClass.doubleType) {
            return "((Double)ret).doubleValue()";
        }
        if (retType == CtClass.charType) {
            return "((Character)ret).charValue()";
        }
        return "(" + retType.getName() + ")ret";
    }

    private static String getParam(CtClass param, int location) {
        int i = location + 1;
        if (param == CtClass.booleanType) {
            return "Boolean.valueOf($" + i + ")";
        }
        if (param == CtClass.byteType) {
            return "Byte.valueOf($" + i + ")";
        }
        if (param == CtClass.shortType) {
            return "Short.valueOf($" + i + ")";
        }
        if (param == CtClass.intType) {
            return "Integer.valueOf($" + i + ")";
        }
        if (param == CtClass.longType) {
            return "Long.valueOf($" + i + ")";
        }
        if (param == CtClass.floatType) {
            return "Float.valueOf($" + i + ")";
        }
        if (param == CtClass.doubleType) {
            return "Double.valueOf($" + i + ")";
        }
        if (param == CtClass.charType) {
            return "Character.valueOf($" + i + ")";
        }
        return "$" + (location + 1);
    }

}
