package com.cloudfuze.mail.exchange.connector;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.microsoft.aad.adal4j.AuthenticationCallback;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;

/**
 * Used to obtain an access token for use in an EWS application. Caches the
 * token and refreshes it 5mins prior to expiration.
 * 
 * @author Stephen O'Hair
 *
 */
@Slf4j
public final class MsEwsTokenProvider {


    private static final String EWS_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
    private static final String RESOUCE = "https://outlook.office365.com";
    private static final String TENANT_NAME = "common";
    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_NAME+"/oauth2/token";
    private static final long REFRESH_BEFORE_EXPIRY_MS = Duration.ofMinutes(5).toMillis();

    private static long expiryTimeMs;
    private static String accessToken;

    /**
     * Takes an OAuth2 token and configures an {@link ExchangeService}.
     * 
     * @param token
     * @param senderAddr
     * @param traceListener
     * @param mailboxAddr
     * @return a configured and authenticated {@link ExchangeService}
     * @throws URISyntaxException
     * @throws Exception
     */
    public static ExchangeService getAuthenticatedService(String token, String senderAddr) throws URISyntaxException, Exception {
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.getHttpHeaders().put("Authorization", "Bearer " + token);
        service.getHttpHeaders().put("X-AnchorMailbox", senderAddr);
        //service.setWebProxy(new WebProxy(proxyHost, proxyPort));
        service.setUrl(new URI(EWS_URL));
      //  service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.PrincipalName, senderAddr));
        return service;
    }
    
    private static String scope = "https://outlook.office.com/.default";

    private static 	String clientId = "dc94adef-3f09-488d-9734-c55b96e614cc";
    private static	String clientSecret = "id08Q~TQL5PlIImBlaOz4DD8M84qjwd-4QIvwcVF";


    public static String getAccesToken() throws Exception {

        ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                clientId,
                ClientCredentialFactory.createFromSecret(clientSecret))
                .authority(AUTHORITY)
                .build();

        // With client credentials flows the scope is ALWAYS of the shape "resource/.default", as the
        // application permissions need to be set statically (in the portal), and then granted by a tenant administrator
        ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
                Collections.singleton(scope))
                .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(clientCredentialParam);
        System.out.println(future.get().accessToken());
        return future.get().accessToken();
    }

    /**
     * Simple way to get an access token using the Azure Active Directory Library.
     * 
     * Authenticates at : https://login.microsoftonline.com/
     * 
     * @param clientId
     *            - client id of the AzureAD application
     * @param clientSecret
     *            - client secret of the AzureAD application
     * @param service
     *            - managed executor service
     * 
     * @return provisioned access token
     * @throws Exception 
     */
    public static synchronized String getAccesToken(String clientId, String clientSecret, ExecutorService service)
            throws Exception {

        long now = System.currentTimeMillis();
        if (accessToken != null && now < expiryTimeMs - REFRESH_BEFORE_EXPIRY_MS) {

            AuthenticationContext context = new AuthenticationContext(AUTHORITY, false, service);
            AuthenticationCallback<AuthenticationResult> callback = new AuthenticationCallback<AuthenticationResult>() {

                @Override
                public void onSuccess(AuthenticationResult result) {
                    log.info("received token");
                }

                @Override
                public void onFailure(Throwable exc) {
                    throw new RuntimeException(exc);
                }
            };

            log.info("requesting token");
            Future<AuthenticationResult> future = context.acquireToken(RESOUCE,
                    new ClientCredential(clientId, clientSecret), callback);

            // wait for access token
            AuthenticationResult result = future.get(30, TimeUnit.SECONDS);

            // cache token and expiration
            accessToken = result.getAccessToken();
            expiryTimeMs = result.getExpiresAfter();
        }else {
        	accessToken = getAccessToken();
        }

        return accessToken;
    }
    
    public static String getAccessToken() throws Exception {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(AUTHORITY);
        
        // Prepare the request body
        String requestBody = "grant_type=client_credentials" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&resource=" + RESOUCE;
        
        httpPost.setEntity(new StringEntity(requestBody));
        
        // Execute the request
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        
        if (entity != null) {
            String responseBody = EntityUtils.toString(entity);
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getString("access_token");
        }
        
        return null;
    }
    
    
}