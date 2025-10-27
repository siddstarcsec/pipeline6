package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/hello")
public class HelloVulnServlet extends HttpServlet {

    private Connection conn;

    @Override
    public void init() throws ServletException {
        try {
            // Initialize in-memory H2 database
            conn = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), bio VARCHAR(255));");
            stmt.execute("INSERT INTO users (username, bio) VALUES ('alice', 'I love cats'), ('bob', 'I love coffee');");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // ---- Reflected XSS vulnerability ----
        String name = request.getParameter("name");
        if (name == null) name = "World12";

        out.println("<h1>Hello, " + name + "!</h1>");
        out.println("<p>Try changing the query string: ?name=YourName</p>");

        // ---- SQL Injection vulnerability ----
        String query = request.getParameter("q");
        if (query != null) {
            out.println("<h2>Search results for: " + query + "</h2>");
            try {
                Statement stmt = conn.createStatement();
                // Vulnerable: concatenating user input into SQL
                String sql = "SELECT username, bio FROM users WHERE username LIKE '%" + query + "%'";
                ResultSet rs = stmt.executeQuery(sql);

                out.println("<ul>");
                while (rs.next()) {
                    out.println("<li>" + rs.getString("username") + " — " + rs.getString("bio") + "</li>");
                }
                out.println("</ul>");
            } catch (SQLException e) {
                out.println("<p>Error: " + e.getMessage() + "</p>");
            }
        }
    }

    @Override
    public void destroy() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
}
