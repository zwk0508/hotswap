package com.zwk.processor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/12/22 14:22
 */

public class ProcessorHelper {
    private static Map<String, Processor> cache = new HashMap<>();

    public static Processor getProcessor(String key) throws Exception {
        Processor processor = cache.get(key);
        if (processor == null) {
            processor = generateProcessor(key);
            cache.put(key, processor);
        }
        return processor;
    }

    private static Processor generateProcessor(String key) throws Exception {
        int index = key.indexOf('(');
        String fileName = key.substring(0, index).replace('.', File.separatorChar) + ".groovy";
        return new ProcessorProxy(fileName);
    }


}
