package com.zwk;

public interface MethodMatcher extends HotSwapMatcher{
    boolean match(String className, int methodAccess, String methodName, String methodDescriptor);
}
