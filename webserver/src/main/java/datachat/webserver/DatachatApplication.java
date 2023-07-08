package datachat.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        excludeName = {
                // to not interfere with target database configuration
                "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration",
        },
        scanBasePackages = {
                "datachat.webserver",
                "datachat.pg",
        }
)
public class DatachatApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatachatApplication.class, args);
    }

}
