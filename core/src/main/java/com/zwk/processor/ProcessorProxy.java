package com.zwk.processor;

import java.io.File;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/12/22 14:30
 */

public class ProcessorProxy implements Processor {
    private String fileName;
    private File file;
    private Processor proxy;

    public ProcessorProxy(String fileName) {
        this.fileName = fileName;
        this.file = new File(fileName);
    }

    @Override
    public boolean isUpdate() {
        return file.exists();
    }

    @Override
    public Object process(Object thiz, Object... args) throws Throwable {
        if (isUpdate()){
            if (proxy == null) {
                proxy = new GroovyFileProcessor(fileName);
            }
            return proxy.process(thiz, args);
        }
        return null;
    }
}
