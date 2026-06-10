package tech.muiru.ncba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NcbaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NcbaApplication.class, args);
    }
}
