package com.bstudio.ro_toolbox.controller;

import com.bstudio.ro_toolbox.service.loginManager.LoginManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/login")
@RequiredArgsConstructor
public class LoginServiceController {

    private final LoginManagerService loginManagerService;

    @GetMapping
    public List<LoginManagerService.LoginAccount> listAccounts() throws IOException {
        return loginManagerService.listAccounts();
    }

    @GetMapping("/quick")
    public List<LoginManagerService.LoginAccount> listQuickAccounts() throws IOException {
        return loginManagerService.listQuickAccounts();
    }

    @PostMapping
    public LoginManagerService.LoginAccount createAccount(@RequestBody LoginManagerService.CreateAccountRequest request) throws IOException {
        return loginManagerService.createAccount(request);
    }

    @PutMapping("/{id}")
    public LoginManagerService.LoginAccount updateAccount(
            @PathVariable String id,
            @RequestBody LoginManagerService.UpdateAccountRequest request
    ) throws IOException {
        return loginManagerService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public LoginManagerService.LoginAccount deleteAccount(@PathVariable String id) throws IOException {
        return loginManagerService.deleteAccount(id);
    }
}
