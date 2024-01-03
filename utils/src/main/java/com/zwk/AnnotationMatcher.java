package com.zwk;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/1/2 18:37
 */

public interface AnnotationMatcher extends HotSwapMatcher {
    /**
     * annotationClassName format like `Lcom/zwk/annotation/HotSwap;`
     * use org.objectweb.asm.Type#getDescriptor(java.lang.Class) get annotation class descriptor
     */
    boolean match(String annotationClassName);
}
