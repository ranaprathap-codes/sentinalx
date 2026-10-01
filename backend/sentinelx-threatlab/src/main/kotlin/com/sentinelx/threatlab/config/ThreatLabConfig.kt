package com.sentinelx.threatlab.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["com.sentinelx.threatlab.repository"])
@EnableTransactionManagement
class ThreatLabConfig