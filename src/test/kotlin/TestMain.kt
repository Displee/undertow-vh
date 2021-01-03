import com.displee.undertow.server.UndertowVH
import com.displee.undertow.util.ResourceUtils
import com.displee.web.localhost.LocalHost

fun main() {
    ResourceUtils.cache = false
    val server = UndertowVH("0.0.0.0", 80, 443)
    server.register(LocalHost())
    server.start()
}