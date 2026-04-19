package lk.tech.moonauth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class EmailConfiguration {

    @Bean
    public EmailClient emailClient(@Value("${app.mail-service.url}") String emailServiceUrl) {
        RestClient restClient = RestClient.builder()
                .baseUrl(emailServiceUrl)
                .build();

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory.builderFor(
                        RestClientAdapter.create(restClient)
                ).build();

        return factory.createClient(EmailClient.class);
    }
}
