package lk.tech.moonauth.email;

import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.bind.annotation.RequestBody;

@HttpExchange("/api/v1/email")
public interface EmailClient {

    @PostExchange("/send")
    void send(@RequestBody EmailRequest request);
}