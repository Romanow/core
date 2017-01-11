package ru.romanow.core.commons.annotations;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by ronin on 10.01.17
 */
@ActiveProfiles("aspect-test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AspectConfiguration.class)
public class LogRequestAspectTest {

    @Autowired
    private AspectTestClass aspectTestClass;

    @Mock
    protected Appender<ILoggingEvent> mockAppender;

    @Captor
    protected ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @Before
    public void init() {
        Logger logger = (Logger)LoggerFactory.getLogger(AspectTestClass.class);
        logger.addAppender(mockAppender);
    }

    @Test
    public void testAspect() {
        String request = RandomStringUtils.randomAlphanumeric(10);
        String response = aspectTestClass.request(request);

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> values = captorLoggingEvent.getAllValues();
        assertTrue(values.get(0).getFormattedMessage().contains(request));
        assertTrue(values.get(1).getFormattedMessage().contains(response));
    }
}