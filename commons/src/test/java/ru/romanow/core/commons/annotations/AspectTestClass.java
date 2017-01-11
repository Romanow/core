package ru.romanow.core.commons.annotations;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by ronin on 10.01.17
 */
@RequestMapping("/test")
class AspectTestClass {

    @LogRequest(classLogger = AspectTestClass.class)
    @RequestMapping("/exists")
    public String requestExists(@Validated @RequestParam Integer id, @RequestBody String request) {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    @LogRequest(classLogger = AspectTestClass.class)
    @RequestMapping("/absent")
    public String requestAbsent(@Validated @RequestParam Integer id) {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    @LogRequest(classLogger = AspectTestClass.class)
    @RequestMapping("/empty")
    public void requestEmpty(@Validated @RequestParam Integer id) {}
}
