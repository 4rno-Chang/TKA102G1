package com.bistroops.sms;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bistroops.employee.model.EmployeeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@ConditionalOnProperty(
        prefix = "app.sms",
        name = "mode",
        havingValue = "console",
        matchIfMissing = true)
public class SmsSimulationController {

    private final ConsoleSmsSender sender;
    private final EmployeeService employeeService;

    public SmsSimulationController(
            ConsoleSmsSender sender,
            EmployeeService employeeService) {

        this.sender = sender;
        this.employeeService = employeeService;
    }

    @GetMapping("/sms/simulation/messages")
    public ResponseEntity<?> messages(
            @RequestParam(name = "staff", defaultValue = "false")
            boolean staff,
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        boolean includeScheduled =
                staff && canReadScheduledMessages(session);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of(
                        "streamId", sender.getStreamId(),
                        "messages",
                        sender.readMessages(session, includeScheduled)));
    }

    private boolean canReadScheduledMessages(HttpSession session) {

        if (session == null) return false;

        Object storedNo = session.getAttribute("employeeNo");
        Object storedExpireAt = session.getAttribute("employeeExpireAt");

        if (!(storedNo instanceof Integer employeeNo)
                || !(storedExpireAt instanceof Long expireAt)
                || System.currentTimeMillis() >= expireAt) {

            return false;
        }

        var auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !employeeNo.toString().equals(auth.getName())
                || auth.getAuthorities().stream().noneMatch(
                        authority ->
                                "EMPLOYEE".equals(authority.getAuthority()))) {

            return false;
        }

        var employee = employeeService.getEmployeeById(employeeNo)
                .orElse(null);

        return employee != null
                && "1".equals(employee.getEmpStatus())
                && employeeService.hasPermission(employeeNo, 4);
    }
}