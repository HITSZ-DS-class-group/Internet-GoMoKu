
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;  
import javax.servlet.http.HttpServletRequest;  
import javax.servlet.http.HttpServletResponse;  
import java.io.IOException;  

@WebServlet("/gomoku")
public class MyServlet extends HttpServlet {  
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {  
        // 处理GET请求的逻辑  
        response.getWriter().write("Hello from MyServlet!");  
    }  

    // 也可以添加doPost等其他方法  
}