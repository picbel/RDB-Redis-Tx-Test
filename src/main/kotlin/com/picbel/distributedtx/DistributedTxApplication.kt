package com.picbel.distributedtx

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DistributedTxApplication

fun main(args: Array<String>) {
    runApplication<DistributedTxApplication>(*args)
}
