package c4compile.async.tokenserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TokenServerApplication

fun main(args: Array<String>) {
	runApplication<TokenServerApplication>(*args)
}
