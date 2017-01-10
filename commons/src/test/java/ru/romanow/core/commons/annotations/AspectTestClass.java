package ru.romanow.core.commons.annotations;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by ronin on 10.01.17
 */
@Component
class AspectTestClass {

    @LogRequest(classLogger = AspectTestClass.class)
    @RequestMapping("/test")
    public String request(@RequestBody String request) {
        return RandomStringUtils.randomAlphanumeric(10);
    }
}
