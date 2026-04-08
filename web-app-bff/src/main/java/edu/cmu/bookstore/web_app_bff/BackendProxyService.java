package edu.cmu.bookstore.web_app_bff;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BackendProxyService {

    private final RestTemplate restTemplate;
    private final String backendBaseUrl;

    public BackendProxyService(RestTemplate restTemplate, @Value("${backend.base-url}") String backendBaseUrl) {
        this.restTemplate = restTemplate;
        this.backendBaseUrl = backendBaseUrl.endsWith("/")
            ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
            : backendBaseUrl;
    }

    public ResponseEntity<String> forward(HttpServletRequest request) throws IOException {
        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        HttpHeaders outboundHeaders = copyRequestHeaders(request);
        HttpEntity<byte[]> entity = body.length == 0
            ? new HttpEntity<>(outboundHeaders)
            : new HttpEntity<>(body, outboundHeaders);

        ResponseEntity<String> backendResponse = restTemplate.exchange(
            buildTargetUri(request),
            HttpMethod.valueOf(request.getMethod()),
            entity,
            String.class
        );

        return adaptResponse(request, backendResponse);
    }

    private URI buildTargetUri(HttpServletRequest request) {
        return UriComponentsBuilder.fromHttpUrl(backendBaseUrl)
            .path(request.getRequestURI())
            .query(request.getQueryString())
            .build(true) // Components are already encoded by the incoming request
            .toUri();
    }

    private HttpHeaders copyRequestHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames == null) {
            return headers;
        }

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (HttpHeaders.HOST.equalsIgnoreCase(headerName) || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)) {
                continue;
            }

            List<String> values = Collections.list(request.getHeaders(headerName));
            headers.put(headerName, values);
        }

        return headers;
    }

    private ResponseEntity<String> adaptResponse(HttpServletRequest request, ResponseEntity<String> backendResponse) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(backendResponse.getHeaders());
        headers.remove(HttpHeaders.TRANSFER_ENCODING);

        URI location = backendResponse.getHeaders().getLocation();
        if (location != null) {
            // This keeps clients on the public BFF address instead of exposing the internal backend URL.
            headers.setLocation(
                ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(location.getRawPath())
                    .replaceQuery(location.getRawQuery())
                    .build(true)
                    .toUri()
            );
        }

        return new ResponseEntity<>(backendResponse.getBody(), headers, backendResponse.getStatusCode());
    }
}
