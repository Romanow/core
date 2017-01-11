package ru.romanow.core.commons.annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by romanow on 11.01.17
 */
@RunWith(MockitoJUnitRunner.class)
@PrepareForTest(Method.class)
public class AspectHelperTest {

    @Test
    public void testGetAnnotatedObject() throws Exception {
        final Object[] args = new Integer[] { 1, 2 };

        final JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(args);

        MethodSignature methodSignature = mock(MethodSignature.class);
        final Method method = this.getClass().getDeclaredMethod("testMethod", Integer.class, Integer.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        Object object = AspectHelper.getAnnotatedObject(joinPoint, RequestBody.class);

        assertEquals(2, object);
    }

    protected void testMethod(@Validated Integer a, @Validated @RequestBody Integer b) {}
}