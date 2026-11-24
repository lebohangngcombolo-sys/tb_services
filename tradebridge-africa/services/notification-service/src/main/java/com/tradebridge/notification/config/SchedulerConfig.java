package com.tradebridge.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * Enables the outbox relay and retry scheduler threads.
 *
 * <p>{@code @EnableScheduling} is already on the main application class;
 * this config only customizes the scheduler pool so that a slow provider
 * call cannot block other scheduled tasks (e.g. the outbox relay).
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "tb-notification-scheduler");
            t.setDaemon(true);
            return t;
        }));
    }
}