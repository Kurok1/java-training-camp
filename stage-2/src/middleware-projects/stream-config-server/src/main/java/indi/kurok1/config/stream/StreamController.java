package indi.kurok1.config.stream;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
@RestController
public class StreamController {

    private ThreadLocalRandom random = ThreadLocalRandom.current();

    @GetMapping("/config")
    public void getConfig(HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        while (true) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            int value = random.nextInt(100);
            response.getWriter().println("{\"key\": \"" + value + "\"}");
            response.flushBuffer();
        }
    }

}
