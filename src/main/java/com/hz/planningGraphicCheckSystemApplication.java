package com.hz;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author saber
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class planningGraphicCheckSystemApplication {
    public static void main(String[] args){
        SpringApplication.run(planningGraphicCheckSystemApplication.class, args);
    }
}