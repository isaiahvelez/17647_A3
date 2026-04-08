package edu.cmu.bookstore.web_app_bff;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxyController {

    private final BackendProxyService backendProxyService;

    public ProxyController(BackendProxyService backendProxyService) {
        this.backendProxyService = backendProxyService;
    }

    @RequestMapping(
        value = {"/books", "/books/**", "/customers", "/customers/**"},
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT}
    )
    public ResponseEntity<String> proxy(HttpServletRequest request) throws IOException {
        return backendProxyService.forward(request);
    }
}
