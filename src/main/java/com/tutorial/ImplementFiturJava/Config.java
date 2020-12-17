package com.tutorial.ImplementFiturJava;

import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineSignatureValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:application.properties")
public class Config {

    @Autowired
    private Environment mEnv;

    @Bean(name = "com.linecorp.channel_secret")
    public String getChannelSecret() {
        return mEnv.getProperty("com.linecorp.channel_secret");
    }

    @Bean(name = "com.linecorp.channel_access_token")
    public String getChannelAccessToken() {
        return mEnv.getProperty("com.linecorp.channel_access_token");
    }

    @Bean(name = "lineMessagingClient")
    public LineMessagingClient getMessagingClient() {
        return LineMessagingClient
                .builder(getChannelAccessToken())
                .apiEndPoint("https://api.line.me/")
                .connectTimeout(50_000)
                .readTimeout(50_000)
                .writeTimeout(50_000)
                .build();
    }

    @Bean(name = "lineSignatureValidator")
    public LineSignatureValidator getSignatureValidator() {
        return new LineSignatureValidator(getChannelSecret().getBytes());
    }
}
