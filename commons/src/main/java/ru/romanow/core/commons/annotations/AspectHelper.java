package ru.romanow.core.commons.annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

/**
 * Created by ronin on 10.01.17
 */
class AspectHelper {

    static Object getAnnotatedObject(ProceedingJoinPoint joinPoint, Class<?> annotationType) {
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
