package ru.romanow.core.commons.annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.springframework.util.StringUtils.hasText;
import static ru.romanow.core.commons.utils.JsonSerializer.toPrettyJson;

@Aspect
public class LogRequestAspect {

    @Before("@annotation(ru.romanow.core.commons.annotations.LogRequest)")
    public void logRequest(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Logger logger = getLogger(method);
        if (logger.isDebugEnabled()) {
            String endpoint = getEndpoint(joinPoint.getTarget(), method);
            Object object = AspectHelper.getAnnotatedObject(joinPoint, RequestBody.class);
            if (object != null) {
                logger.debug("Request for endpoint [{}]:\n{}", endpoint, toPrettyJson(object));
            }
        }
    }

    @AfterReturning(value = "@annotation(ru.romanow.core.commons.annotations.LogRequest)", returning = "result")
    public void logResponse(JoinPoint joinPoint, Object result) {
        if (result != null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Logger logger = getLogger(method);
            if (logger.isDebugEnabled()) {
                String endpoint = getEndpoint(joinPoint.getTarget(), method);
                logger.debug("Returning result for endpoint [{}]:\n{}", endpoint, toPrettyJson(result));
            }
        }
    }

    private String getEndpointPath(RequestMapping requestMapping) {
        return notEmpty(requestMapping.value()) ? requestMapping.value()[0] : "";
    }

    private boolean notEmpty(String[] value) {
        return value != null && value.length > 0;
    }

    private Logger getLogger(Method method) {
        LogRequest logRequest = method.getAnnotation(LogRequest.class);
        return LoggerFactory.getLogger(logRequest.classLogger());
    }

    private String getEndpoint(Object targetClass, Method method) {
        RequestMapping classMapping = targetClass.getClass().getAnnotation(RequestMapping.class);
        String classEndpoint = null;
        if (classMapping != null) {
            classEndpoint = getEndpointPath(classMapping);
        }
        RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);
        String methodEndpoint = getEndpointPath(methodMapping);

        return hasText(classEndpoint) ?
                classEndpoint + methodEndpoint : methodEndpoint;
    }
}
