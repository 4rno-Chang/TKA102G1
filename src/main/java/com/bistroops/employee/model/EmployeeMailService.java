package com.bistroops.employee.model;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmployeeMailService {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final String loginUrl;

    public EmployeeMailService(
            @Value("${employee.mail.host:${EMPLOYEE_MAIL_HOST:smtp.gmail.com}}") String host,
            @Value("${employee.mail.port:${EMPLOYEE_MAIL_PORT:587}}") int port,
            @Value("${employee.mail.username:${EMPLOYEE_MAIL_USERNAME:}}") String username,
            @Value("${employee.mail.password:${EMPLOYEE_MAIL_PASSWORD:}}") String password,
            @Value("${employee.mail.from:${EMPLOYEE_MAIL_FROM:}}") String from,
            @Value("${employee.login-url:${EMPLOYEE_LOGIN_URL:http://localhost:8080/staff/login}}") String loginUrl) {
        this.host = host.isBlank() ? "smtp.gmail.com" : host.trim();
        this.port = port;
        this.username = username.trim();
        this.password = "smtp.gmail.com".equalsIgnoreCase(this.host)
                ? password.replace(" ", "").trim() : password;
        this.from = from.isBlank() ? this.username : from.trim();
        this.loginUrl = loginUrl;
    }

    public boolean sendTemporaryPassword(EmployeeVO employee, String temporaryPassword) {
        if (host.isBlank() || username.isBlank() || password.isBlank() || from.isBlank()
                || employee.getEmpEmail() == null || employee.getEmpEmail().isBlank()) {
            return false;
        }
        try {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", host);
            properties.setProperty("mail.smtp.port", Integer.toString(port));
            properties.setProperty("mail.smtp.auth", "true");
            properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
            if (port == 465) {
                properties.setProperty("mail.smtp.ssl.enable", "true");
            } else {
                properties.setProperty("mail.smtp.starttls.enable", "true");
                properties.setProperty("mail.smtp.starttls.required", "true");
            }
            properties.setProperty("mail.smtp.connectiontimeout", "5000");
            properties.setProperty("mail.smtp.timeout", "10000");
            properties.setProperty("mail.smtp.writetimeout", "10000");
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(employee.getEmpEmail(), true));
            message.setSubject("Bistroops 員工帳號登入通知", StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            message.setText(buildBody(employee, temporaryPassword), StandardCharsets.UTF_8.name());
            Transport.send(message);
            return true;
        } catch (Exception exception) {
            // Do not log SMTP credentials, recipient details, or the temporary password.
            return false;
        }
    }

    String buildBody(EmployeeVO employee, String temporaryPassword) {
        return employee.getEmpName() + " 您好：\n\n"
                + "您的員工帳號已建立、恢復在職或重設密碼。\n"
                + "員工登入網址：" + loginUrl + "\n"
                + "員工帳號：" + employee.getEmpNo() + "\n"
                + "臨時密碼：" + temporaryPassword + "\n\n"
                + "臨時密碼自建立起 24 小時內有效，首次登入後必須設定新密碼。\n"
                + "若通知信多次寄送，請使用最新一封信的密碼。\n"
                + "若無法登入或密碼已過期，請聯絡主管重新寄送。\n"
                + "請勿轉寄此信或將密碼提供給他人。\n";
    }
}
