package com.zwk.processor;

/**
 * @author zwk
 * @version 1.0
 * @date 2023/12/14 17:41
 */

public interface Processor {

    boolean isUpdate();

    Object process(Object thiz, Object... args) throws Throwable;

}
