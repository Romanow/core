package ru.romanow.core.spring.rest.client;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TestWebConfiguration.class)
public class TestRestServerConfiguration {}
