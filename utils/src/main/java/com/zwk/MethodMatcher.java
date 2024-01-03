package com.zwk;

public interface MethodMatcher extends HotSwapMatcher {
    /**
     * className format like `com/zwk/annotation/HotSwap`
     * methodName fortmat like `match`
     * methodDescriptor format like `(Ljava/lang/String;)Ljava/lang/Object;`
     */
    boolean match(String className, int methodAccess, String methodName, String methodDescriptor);
}
