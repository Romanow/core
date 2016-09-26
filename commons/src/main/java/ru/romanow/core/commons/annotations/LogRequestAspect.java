package ru.romanow.core.commons.annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Before;

/**
 * Created by ronin on 20.09.16
 */
public class LogRequestAspect {

    @Before("@annotations(ru.romanow.core.annotations.LogRequest)")
    public void logRequest(JoinPoint joinPoint) {

    }

    @AfterReturning(value = "@annotations(ru.romanow.core.annotations.LogRequest)", returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {

    }
}
