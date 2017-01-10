package ru.romanow.core.commons.annotations;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by ronin on 10.01.17
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AspectConfiguration.class)
public class LogRequestAspectTest {

    @Autowired
    private AspectTestClass aspectTestClass;

    @Test
    public void testAspect() {
        aspectTestClass.request(RandomStringUtils.randomAlphanumeric(10));
    }
}