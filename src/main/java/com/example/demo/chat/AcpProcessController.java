package com.example.demo.chat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.acp.AcpAgent;
import com.example.demo.chat.dto.AcpProcessStatus;

/** Lets the chat UI check whether the ACP agent process is running, and start/kill it on demand. */
@RestController
@RequestMapping("/api/agent/process")
public class AcpProcessController {

    private final AcpAgent acpAgent;

    public AcpProcessController(AcpAgent acpAgent) {
        this.acpAgent = acpAgent;
    }

    @GetMapping
    public AcpProcessStatus status() {
        return new AcpProcessStatus(acpAgent.isAlive());
    }

    @PostMapping("/start")
    public AcpProcessStatus start() {
        acpAgent.startProcess();
        return new AcpProcessStatus(acpAgent.isAlive());
    }

    @PostMapping("/stop")
    public AcpProcessStatus stop() {
        acpAgent.stopProcess();
        return new AcpProcessStatus(acpAgent.isAlive());
    }
}
