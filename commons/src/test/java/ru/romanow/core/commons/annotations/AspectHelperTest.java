package ru.romanow.core.commons.annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AspectHelperTest {

    @Test
    public void testGetAnnotatedObjectExists() throws Exception {
        final Object[] args = new Integer[] { 1, 2 };
        final Method method = this.getClass().getDeclaredMethod("testMethodExists", Integer.class, Integer.class);
        Object object = getAnnotatedObject(args, method);

        assertEquals(2, object);
    }

    @Test
    public void testGetAnnotatedObjectAbsent() throws Exception {
        final Object[] args = new Integer[] { 1, 2 };
        final Method method = this.getClass().getDeclaredMethod("testMethodAbsent", Integer.class, Integer.class);
        Object object = getAnnotatedObject(args, method);

        assertNull(object);
    }

    protected void testMethodExists(@Validated Integer a, @Validated @RequestBody Integer b) {}

    protected void testMethodAbsent(@Validated Integer a, @Validated Integer b) {}

    private Object getAnnotatedObject(final Object[] args, final Method method) {
        final JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(args);

        MethodSignature methodSignature = mock(MethodSignature.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);

        return AspectHelper.getAnnotatedObject(joinPoint, RequestBody.class);
    }
}