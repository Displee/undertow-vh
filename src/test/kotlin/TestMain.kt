import com.displee.undertow.server.UndertowVH
import com.displee.web.localhost.LocalHost

fun main() {
    val controller = UndertowVH("0.0.0.0", 80)
    controller.register(LocalHost())
    controller.initialize()
}