package cn.zhijie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.jobs-enabled",havingValue="true",matchIfMissing=true)
@EnableScheduling
public class SchedulingConfig {}
