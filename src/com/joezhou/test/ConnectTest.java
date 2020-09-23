package com.joezhou.test;

import lombok.SneakyThrows;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author JoeZhou
 */
public class ConnectTest {

    @SneakyThrows
    @Test
    public void connectMySql() {
        String user = "joezhou";
        String password = "joezhou";
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/dbjoe";
        String urlParam = "?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
        url += urlParam;
        Class.forName(driver);
        Connection connection = DriverManager.getConnection(url, user, password);
        System.out.println(connection.isClosed() ? "fail" : "success");
    }

    @SneakyThrows
    @Test
    public void connectOracle() {
        String user = "joezhou";
        String password = "joezhou";
        String driver = "oracle.jdbc.driver.OracleDriver";
        String url = "jdbc:oracle:thin:@localhost:1521:dbjoe";
        Class.forName("driver");
        Connection connection = DriverManager.getConnection(url, user, password);
        System.out.println(connection.isClosed() ? "fail" : "success");
    }

}
