package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.security.KeyManagementException;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

@SpringBootApplication
@RestController
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.customizers(restTemplateCustomizer()).build();
	}

	@Bean
	public RestTemplateCustomizer restTemplateCustomizer() {
		return restTemplate -> {
			try {
				KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
				FileInputStream clientKeyStoreFile = new FileInputStream("/Users/tharmigan/Downloads/SSLBackendIssue/wso2am-micro-gw-macos-3.2.0.92-SNAPSHOT/runtime/bre/security/ballerinaKeystore.p12");
				clientKeyStore.load(clientKeyStoreFile, "ballerina".toCharArray());

				KeyStore trustStore = KeyStore.getInstance("PKCS12");
				FileInputStream trustStoreFile = new FileInputStream("/Users/tharmigan/Downloads/SSLBackendIssue/wso2am-micro-gw-macos-3.2.0.92-SNAPSHOT/runtime/bre/security/ballerinaTruststore.p12");
				trustStore.load(trustStoreFile, "ballerina".toCharArray());

				SSLContext sslContext = SSLContexts.custom()
						.loadKeyMaterial(clientKeyStore, "ballerina".toCharArray())
						.loadTrustMaterial(trustStore, null)
						.build();

				SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
				CloseableHttpClient httpClient = HttpClients.custom()
						.setSSLSocketFactory(sslSocketFactory)
						.build();

				HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
				restTemplate.setRequestFactory(factory);

				restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
					@Override
					public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
						request.getHeaders().add("X-Forwarded-Host", request.getURI().getHost());
						return execution.execute(request, body);
					}
				}));

			} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
				throw new RuntimeException("Error configuring SSL", e);
			}
		};
	}

	@RequestMapping("/**")
	public String forwardRequest(RestTemplate restTemplate, HttpServletRequest request) {
		String queryString = request.getQueryString();
		String backendUrl = "https://petstore3.swagger.io" + request.getRequestURI() + (queryString != null ? "?" + queryString : "");
		return restTemplate.getForObject(backendUrl, String.class);
	}
}