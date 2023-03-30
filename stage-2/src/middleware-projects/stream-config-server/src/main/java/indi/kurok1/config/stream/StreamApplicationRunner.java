package indi.kurok1.config.stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
//@Component
public class StreamApplicationRunner implements ApplicationRunner {

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        restTemplate.execute("http://localhost:9090/config", HttpMethod.GET, request -> {
        }, response -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody()));
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                    System.out.println("Got some data, let's use my ObjectMapper to parse into something useful!");
                }
            } catch (IOException e) {
                //Something clever
            }
            return response;
        });
    }
}
