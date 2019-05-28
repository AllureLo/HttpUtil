package com.callenled.util.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @Author: Callenld
 * @Date: 19-5-28
 */
public abstract class TypeToken<T> {
    private final Type type;

    public TypeToken() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new TypeException("No generics found!");
        }
        ParameterizedType type = (ParameterizedType) superclass;
        this.type = type.getActualTypeArguments()[0];
    }

    public Type getType() {
        return type;
    }
}
