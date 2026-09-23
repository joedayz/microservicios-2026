package pe.joedayz.microservicios.observability.flaky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlakyDownstreamApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlakyDownstreamApplication.class, args);
    }
}
