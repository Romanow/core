package ru.romanow.core.commons.annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

class AspectHelper {

    static Object getAnnotatedObject(JoinPoint joinPoint, Class<?> annotationType) {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
        for (int i = 0; i < annotations.length; ++i) {
            if (Stream.of(annotations[i])
                      .anyMatch(a -> a.annotationType().equals(annotationType))) {
                return args[i];
            }
        }

        return null;
    }
}
