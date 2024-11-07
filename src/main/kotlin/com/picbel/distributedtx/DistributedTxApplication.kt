package com.picbel.distributedtx

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@EnableJpaRepositories
@SpringBootApplication
class DistributedTxApplication

fun main(args: Array<String>) {
    runApplication<DistributedTxApplication>(*args)
}
