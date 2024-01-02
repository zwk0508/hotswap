package com.zwk;

import com.zwk.annotation.HotSwap;
import org.objectweb.asm.Type;

public interface HotSwapMatcher {
    default boolean match(String className, int methodAccess, String methodName, String methodDescriptor) {
        return false;
    }

   default boolean match(String annotationClassName){
       return false;
   }

    HotSwapMatcher DEFAULT_MATCHER = (AnnotationMatcher) annotationClassName -> Type.getDescriptor(HotSwap.class).equals(annotationClassName);
}
