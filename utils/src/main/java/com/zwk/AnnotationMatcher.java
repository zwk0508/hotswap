package com.zwk;

/**
 * @author zwk
 * @version 1.0
 * @date 2024/1/2 18:37
 */

public interface AnnotationMatcher extends HotSwapMatcher {
    boolean match(String annotationClassName);
}
