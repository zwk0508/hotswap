package com.zwk;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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
        File file = new File(dir);
        processDir(file);
    }

    private static void processDir(File file) throws Exception {
        if (!file.isDirectory()) {
            if (!file.getName().endsWith(".class")) {
                return;
            }
            byte[] bytes = Files.readAllBytes(file.toPath());
            ClassReader cr = new ClassReader(bytes, 0, bytes.length);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            MyClassVisitor classVisitor = new MyClassVisitor(Opcodes.ASM9, cw);
            cr.accept(classVisitor, ClassReader.EXPAND_FRAMES);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(cw.toByteArray());
            }
        } else {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    processDir(file1);
                }
            }
        }


    }


    private static void processJarFile(String jarFileName) throws Exception {
        File file = new File(jarFileName);
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        Map<String, byte[]> map = new HashMap<>();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                ClassReader cr = new ClassReader(inputStream);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                MyClassVisitor classVisitor = new MyClassVisitor(Opcodes.ASM9, cw);
                cr.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                map.put(jarEntry.getName(), cw.toByteArray());

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

    private static void processFile(String fileName) throws Exception {
        File file = new File(fileName);
        ClassReader cr = new ClassReader(new FileInputStream(file));
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        MyClassVisitor classVisitor = new MyClassVisitor(Opcodes.ASM9, cw);
        cr.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        byte[] bytes = cw.toByteArray();
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
        }
    }

}
