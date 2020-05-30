import com.displee.undertow.server.UndertowVH
import com.displee.web.localhost.LocalHost

fun main() {
    //TODO Add extends and include tests
    val server = UndertowVH("0.0.0.0", 80)
    server.register(LocalHost())
    server.start()
}