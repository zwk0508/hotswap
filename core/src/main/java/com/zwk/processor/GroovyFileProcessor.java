package com.zwk.processor;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/12/14 17:43
 */

public class GroovyFileProcessor implements Processor {

    private File file;
    private long lastModified;
    private GroovyObject groovyObject;

    public GroovyFileProcessor(String fileName) throws IOException, IllegalAccessException, InstantiationException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new FileNotFoundException(fileName);
        }
        this.lastModified = file.lastModified();
        this.file = file;
        load();
    }

    private void load() throws IOException, IllegalAccessException, InstantiationException {
        GroovyClassLoader classLoader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader());

        List<String> lines = Files.readAllLines(file.toPath());
        Iterator<String> iterator = lines.iterator();
        List<String> imports = new ArrayList<>();
        while (iterator.hasNext()) {
            String next = iterator.next();
            next = next.trim();
            if ("".equals(next)) {
                iterator.remove();
                continue;
            }
            if (next.startsWith("import ")) {
                imports.add(next);
                iterator.remove();
                continue;
            }
            break;
        }
        String imps = String.join("\n", imports);
        String body = String.join("\n", lines);

        String script = imps + "\ndef clo = { args ->\n" +
                body +
                "\n}\n" +
                "clo.setResolveStrategy(Closure.DELEGATE_FIRST)\n" +
                "clo.delegate = obj\n" +
                "\n" +
                "clo(args)";

        this.groovyObject = (GroovyObject) classLoader.parseClass(script).newInstance();
    }

    @Override
    public boolean isUpdate() {
        long lastModified = file.lastModified();
        if (lastModified > this.lastModified) {
            this.lastModified = lastModified;
            return true;
        }
        return false;
    }

    @Override
    public Object process(Object obj, Object... args) throws Throwable {
        if (isUpdate()) {
            load();
        }
        groovyObject.setProperty("obj", obj);
        groovyObject.setProperty("args", args);
        return groovyObject.invokeMethod("run", null);
    }
}
