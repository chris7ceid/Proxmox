package org.ceid_uni;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IrisApp {
    public static void main(String[] args) {SpringApplication.run(IrisApp.class, args);}
}