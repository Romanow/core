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

import static org.apache.commons.lang.math.RandomUtils.nextInt;
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
    public void testAspectExists() {
        String request = RandomStringUtils.randomAlphanumeric(10);
        String response = aspectTestClass.requestExists(nextInt(10), request);

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        List<LoggingEvent> values = captorLoggingEvent.getAllValues();
        String formattedMessage = values.get(0).getFormattedMessage();
        assertTrue(formattedMessage.startsWith("Request for endpoint [/test/exists]"));
        assertTrue(formattedMessage.contains(request));

        formattedMessage = values.get(1).getFormattedMessage();
        assertTrue(formattedMessage.startsWith("Returning result for endpoint [/test/exists]"));
        assertTrue(formattedMessage.contains(response));
    }


    @Test
    public void testAspectAbsent() {
        String response = aspectTestClass.requestAbsent(nextInt(10));
        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());

        List<LoggingEvent> values = captorLoggingEvent.getAllValues();
        String formattedMessage = values.get(0).getFormattedMessage();
        assertTrue(formattedMessage.startsWith("Returning result for endpoint [/test/absent]"));
        assertTrue(formattedMessage.contains(response));
    }

    @Test
    public void testAspectEmpty() {
        aspectTestClass.requestEmpty(nextInt(10));
        verify(mockAppender, times(0)).doAppend(captorLoggingEvent.capture());
    }
}