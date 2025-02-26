/*
 * Copyright (c) 2018 - 2023, Entgra (Pvt) Ltd. (http://www.entgra.io) All Rights Reserved.
 *
 * Entgra (Pvt) Ltd. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.entgra.device.mgt.core.apimgt.keymgt.extension.service;

import com.google.gson.Gson;
import io.entgra.device.mgt.core.apimgt.extension.rest.api.ConsumerRESTAPIServices;
import io.entgra.device.mgt.core.apimgt.extension.rest.api.dto.ApiApplicationInfo;
import io.entgra.device.mgt.core.apimgt.extension.rest.api.exceptions.APIServicesException;
import io.entgra.device.mgt.core.apimgt.extension.rest.api.exceptions.UnexpectedResponseException;
import io.entgra.device.mgt.core.apimgt.keymgt.extension.*;
import io.entgra.device.mgt.core.apimgt.keymgt.extension.exception.BadRequestException;
import io.entgra.device.mgt.core.apimgt.keymgt.extension.exception.KeyMgtException;
import io.entgra.device.mgt.core.apimgt.keymgt.extension.internal.KeyMgtDataHolder;
import io.entgra.device.mgt.core.device.mgt.core.config.DeviceConfigurationManager;
import io.entgra.device.mgt.core.device.mgt.core.config.DeviceManagementConfig;
import io.entgra.device.mgt.core.device.mgt.core.config.keymanager.KeyManagerConfigurations;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class KeyMgtServiceImpl implements KeyMgtService {

    private static final Log log = LogFactory.getLog(KeyMgtServiceImpl.class);

    private static final OkHttpClient client = getOkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson gson = new Gson();
    private KeyManagerConfigurations kmConfig = null;
    RealmService realmService = null;
    String subTenantUserUsername, subTenantUserPassword, keyManagerName, msg = null;

    public DCRResponse dynamicClientRegistration(String clientName, String owner, String grantTypes, String callBackUrl,
                                                 String[] tags, boolean isSaasApp, int validityPeriod,
                                                 String password, List<String> supportedGrantTypes, String callbackUrl) throws KeyMgtException {

        if (owner == null) {
            PrivilegedCarbonContext threadLocalCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            try {
                owner = APIUtil.getTenantAdminUserName(threadLocalCarbonContext.getTenantDomain());
            } catch (APIManagementException e) {
                String msg = "Error occurred while retrieving admin user for the tenant " + threadLocalCarbonContext.getTenantDomain();
                log.error(msg, e);
                throw new KeyMgtException(msg);
            }
        }

        String tenantDomain = MultitenantUtils.getTenantDomain(owner);
        int tenantId;

        try {
            tenantId = getRealmService()
                    .getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            msg = "Error while loading tenant configuration";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }

        kmConfig = getKeyManagerConfig();

        if (KeyMgtConstants.SUPER_TENANT.equals(tenantDomain)) {
            OAuthApplication dcrApplication = createOauthApplication(clientName, kmConfig.getAdminUsername(), tags,
                    validityPeriod, kmConfig.getAdminPassword(), supportedGrantTypes, callbackUrl);
            return new DCRResponse(dcrApplication.getClientId(), dcrApplication.getClientSecret());
        } else {
            // super-tenant admin dcr and token generation
            OAuthApplication superTenantOauthApp = createOauthApplication(
                    KeyMgtConstants.RESERVED_OAUTH_APP_NAME_PREFIX + KeyMgtConstants.SUPER_TENANT,
                    kmConfig.getAdminUsername(), null, validityPeriod, kmConfig.getAdminPassword(), null, null);
            String superAdminAccessToken = createAccessToken(superTenantOauthApp);

            // create new key manager for the tenant, under super-tenant space
            createKeyManager(tenantId, tenantDomain, superAdminAccessToken);

            // create a sub-tenant user
            try {
                subTenantUserUsername = getRealmService()
                        .getTenantUserRealm(tenantId).getRealmConfiguration()
                        .getRealmProperty("reserved_tenant_user_username") + "@" + tenantDomain;
                subTenantUserPassword = getRealmService()
                        .getTenantUserRealm(tenantId).getRealmConfiguration()
                        .getRealmProperty("reserved_tenant_user_password");
            } catch (UserStoreException e) {
                msg = "Error while loading user realm configuration";
                log.error(msg, e);
                throw new KeyMgtException(msg, e);
            }
            createUserIfNotExists(subTenantUserUsername, subTenantUserPassword);

            // DCR for the requesting user
            OAuthApplication dcrApplication = createOauthApplication(clientName, owner, tags, validityPeriod,
                    password, null, null);
            String requestingUserAccessToken = createAccessToken(dcrApplication);

            // get application id
            io.entgra.device.mgt.core.apimgt.extension.rest.api.bean.APIMConsumer.Application application =
                    getApplication(clientName, requestingUserAccessToken);
            String applicationUUID = application.getApplicationId();

            // do app key mapping
            mapApplicationKeys(dcrApplication.getClientId(), dcrApplication.getClientSecret(), keyManagerName,
                    applicationUUID, requestingUserAccessToken);
            return new DCRResponse(dcrApplication.getClientId(), dcrApplication.getClientSecret());
        }
    }

    public TokenResponse generateAccessToken(TokenRequest tokenRequest) throws KeyMgtException, BadRequestException {
        try {
            Application application = APIUtil.getApplicationByClientId(tokenRequest.getClientId());
            if (application == null) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("error", "invalid_client");
                errorResponse.put("error_description", "A valid OAuth client could not be found for client_id: "
                        + tokenRequest.getClientId());
                throw new BadRequestException(errorResponse.toString());
            }

            String tenantDomain = MultitenantUtils.getTenantDomain(application.getOwner());
            kmConfig = getKeyManagerConfig();
            String appTokenEndpoint = kmConfig.getServerUrl() + KeyMgtConstants.OAUTH2_TOKEN_ENDPOINT;

            RequestBody appTokenPayload;
            switch (tokenRequest.getGrantType()) {
                case "client_credentials":
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", "client_credentials")
                            .add("scope", tokenRequest.getScope())
                            .add("validityPeriod", String.valueOf(tokenRequest.getValidityPeriod())).build();
                    break;
                case "password":
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", "password")
                            .add("username", tokenRequest.getUsername())
                            .add("password", tokenRequest.getPassword())
                            .add("scope", tokenRequest.getScope()).build();
                    break;
                case "refresh_token":
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", "refresh_token")
                            .add("refresh_token", tokenRequest.getRefreshToken()).build();
                    break;
                case "urn:ietf:params:oauth:grant-type:jwt-bearer":
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                            .add("assertion", tokenRequest.getAssertion())
                            .add("scope", tokenRequest.getScope()).build();
                    appTokenEndpoint += "?tenantDomain=carbon.super";
                    break;
                case "access_token":
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", "access_token")
                            .add("admin_access_token", tokenRequest.getAdminAccessToken())
                            .add("scope", tokenRequest.getScope()).build();
                    break;
                default:
                    appTokenPayload = new FormBody.Builder()
                            .add("grant_type", tokenRequest.getGrantType())
                            .add("scope", tokenRequest.getScope()).build();
                    break;
            }

            Request request = new Request.Builder()
                    .url(appTokenEndpoint)
                    .addHeader(KeyMgtConstants.AUTHORIZATION_HEADER, Credentials.basic(tokenRequest.getClientId(), tokenRequest.getClientSecret()))
                    .post(appTokenPayload)
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject responseObj = new JSONObject(Objects.requireNonNull(response.body()).string());

            if (!response.isSuccessful()) {
                throw new BadRequestException(responseObj.toString());
            }

            String accessToken;
            if (KeyMgtConstants.SUPER_TENANT.equals(tenantDomain)) {
                accessToken = responseObj.getString("access_token");
            } else {
                int tenantId = getRealmService()
                        .getTenantManager().getTenantId(tenantDomain);
                accessToken = tenantId + "_" + responseObj.getString("access_token");
            }

            if (tokenRequest.getGrantType().equals("client_credentials")) {
                return new TokenResponse(accessToken,
                        responseObj.getString("scope"),
                        responseObj.getString("token_type"),
                        responseObj.getInt("expires_in"));
            } else {
                return new TokenResponse(accessToken,
                        responseObj.getString("refresh_token"),
                        responseObj.getString("scope"),
                        responseObj.getString("token_type"),
                        responseObj.getInt("expires_in"));
            }
        } catch (APIManagementException e) {
            msg = "Error occurred while retrieving application";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        } catch (IOException e) {
            msg = "Error occurred while mapping application keys";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        } catch (UserStoreException e) {
            msg = "Error occurred while fetching tenant id";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Maps the application's keys with the given key manager
     *
     * @param consumerKey consumer key of the application
     * @param consumerSecret consumer secret of the application
     * @param keyManager key-manager name to which the keys should be mapped with
     * @param applicationUUID application's UUID
     * @param accessToken access token of the tenant user
     * @throws KeyMgtException if an error occurs while mapping application keys with the key-manager
     */
    private void mapApplicationKeys(String consumerKey, String consumerSecret, String keyManager,
                                    String applicationUUID, String accessToken) throws KeyMgtException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("consumerKey", consumerKey);
        jsonObject.put("consumerSecret", consumerSecret);
        jsonObject.put("keyManager", keyManager);
        jsonObject.put("keyType", "PRODUCTION");

        RequestBody keyMappingPayload = RequestBody.Companion.create(jsonObject.toString(), JSON);
        kmConfig = getKeyManagerConfig();
        String keyMappingEndpoint = kmConfig.getServerUrl() +
                KeyMgtConstants.APPLICATION_KEY_MAPPING_ENDPOINT.replaceAll("<applicationId>", applicationUUID);
        Request request = new Request.Builder()
                .url(keyMappingEndpoint)
                .addHeader(KeyMgtConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                .addHeader(KeyMgtConstants.X_WSO2_TENANT_HEADER, KeyMgtConstants.SUPER_TENANT)
                .post(keyMappingPayload)
                .build();

        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            msg = "Error occurred while mapping application keys";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Creates user if not exists already in the user store
     *
     * @param username username of the user
     * @param password password of the user
     * @throws KeyMgtException if any error occurs while fetching tenant details
     */
    private void createUserIfNotExists(String username, String password) throws KeyMgtException {
        try {
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            int tenantId = getRealmService()
                    .getTenantManager().getTenantId(tenantDomain);
            UserRealm userRealm = getRealmService()
                    .getTenantUserRealm(tenantId);
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();

            if (!userStoreManager.isExistingUser(MultitenantUtils.getTenantAwareUsername(username))) {
                String[] roles = {"admin"};
                userStoreManager.addUser(MultitenantUtils.getTenantAwareUsername(username), password, roles, null, "");
            }
        } catch (UserStoreException e) {
            msg = "Error when trying to fetch tenant details";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Creates an OAuth Application
     *
     * @param clientName Name of the client application
     * @param owner Owner's name of the client application
     * @return @{@link OAuthApplication} OAuth application object
     * @throws KeyMgtException if any error occurs while creating response object
     */
    private OAuthApplication createOauthApplication (String clientName, String owner, String[] tags,
                                                     int validityPeriod, String ownerPassword,
                                                     List<String> supportedGrantTypes, String callbackUrl) throws KeyMgtException {
        String oauthAppCreationPayloadStr = createOauthAppCreationPayload(clientName, owner, tags, validityPeriod,
                ownerPassword, supportedGrantTypes, callbackUrl);
        RequestBody oauthAppCreationPayload = RequestBody.Companion.create(oauthAppCreationPayloadStr, JSON);
        kmConfig = getKeyManagerConfig();
        String dcrEndpoint = kmConfig.getServerUrl() + KeyMgtConstants.DCR_ENDPOINT;
        String username, password;

        if (KeyMgtConstants.SUPER_TENANT.equals(MultitenantUtils.getTenantDomain(owner))) {
            username = kmConfig.getAdminUsername();
            password = kmConfig.getAdminPassword();
        } else {
            username = subTenantUserUsername;
            password = subTenantUserPassword;
        }

        Request request = new Request.Builder()
                .url(dcrEndpoint)
                .addHeader(KeyMgtConstants.AUTHORIZATION_HEADER, Credentials.basic(username, password))
                .post(oauthAppCreationPayload)
                .build();
        try {
            Response response = client.newCall(request).execute();
            return gson.fromJson(response.body().string(), OAuthApplication.class);
        } catch (IOException e) {
            msg = "Error occurred while processing the response." ;
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Creates access token with client credentials grant type
     *
     * @param oAuthApp OAuth application object
     * @return Access token
     * @throws KeyMgtException if any error occurs while reading access token from the response
     */
    private String createAccessToken (OAuthApplication oAuthApp) throws KeyMgtException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("grant_type", KeyMgtConstants.CLIENT_CREDENTIALS_GRANT_TYPE);
        jsonObject.put("scope", KeyMgtConstants.DEFAULT_ADMIN_SCOPES);

        RequestBody accessTokenReqPayload = RequestBody.Companion.create(jsonObject.toString(), JSON);
        kmConfig = getKeyManagerConfig();
        String tokenEndpoint = kmConfig.getServerUrl() + KeyMgtConstants.OAUTH2_TOKEN_ENDPOINT;
        Request request = new Request.Builder()
                .url(tokenEndpoint)
                .addHeader(KeyMgtConstants.AUTHORIZATION_HEADER, Credentials.basic(oAuthApp.getClientId(), oAuthApp.getClientSecret()))
                .post(accessTokenReqPayload)
                .build();

        try {
            Response response = client.newCall(request).execute();
            jsonObject = new JSONObject(response.body().string());
            return jsonObject.getString("access_token");
        } catch (IOException e) {
            msg = "Error occurred while reading access token from response";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Creates a key manager for a given tenant, under super-tenant space
     *
     * @param tenantId tenant-id of the key-manager
     * @param tenantDomain tenant domain of the key-manager
     * @param accessToken access token of the super-tenant user
     * @throws KeyMgtException if any error occurs while creating a key-manager
     */
    private void createKeyManager(int tenantId, String tenantDomain, String accessToken) throws KeyMgtException {
        try {
            List<String> kmGrantTypes = new ArrayList<>();
            kmGrantTypes.add("client_credentials");

            kmConfig = getKeyManagerConfig();
            Map<String, Object> additionalProperties = new HashMap<>();
            additionalProperties.put("Username", kmConfig.getAdminUsername());
            additionalProperties.put("Password", kmConfig.getAdminPassword());
            additionalProperties.put("self_validate_jwt", true);

            keyManagerName = generateCustomKeyManagerName(tenantDomain);
            KeyManagerPayload keyManagerPayload = new KeyManagerPayload(
                    tenantDomain, tenantId, kmConfig.getServerUrl(),
                    keyManagerName, kmGrantTypes, additionalProperties
            );
            String createKeyManagerPayload = gson.toJson(keyManagerPayload);
            RequestBody requestBody = RequestBody.Companion.create(createKeyManagerPayload, JSON);
            String keyManagerEndpoint = kmConfig.getServerUrl() + KeyMgtConstants.CREATE_KEY_MANAGER_ENDPOINT;
            Request request = new Request.Builder()
                    .url(keyManagerEndpoint)
                    .addHeader(KeyMgtConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken)
                    .post(requestBody)
                    .build();
            client.newCall(request).execute();
        } catch (IOException e) {
            msg = "Error occurred while invoking create key manager endpoint";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    /***
     * Retrieves an application by name and owner
     *
     * @param applicationName name of the application
     * @param accessToken Access Token
     * @return @{@link Application} Application object
     * @throws KeyMgtException if any error occurs while retrieving the application
     */
    private io.entgra.device.mgt.core.apimgt.extension.rest.api.bean.APIMConsumer.Application getApplication(String applicationName, String accessToken) throws KeyMgtException {

        ApiApplicationInfo apiApplicationInfo = new ApiApplicationInfo();
        apiApplicationInfo.setAccess_token(accessToken);
        try {
            ConsumerRESTAPIServices consumerRESTAPIServices =
                    KeyMgtDataHolder.getInstance().getConsumerRESTAPIServices();
            io.entgra.device.mgt.core.apimgt.extension.rest.api.bean.APIMConsumer.Application[] applications =
                    consumerRESTAPIServices.getAllApplications(applicationName);
            if (applications.length == 1) {
                return applications[0];
            } else {
                String msg =
                        "Found invalid number of applications. No of applications found from the APIM: " + applications.length;
                log.error(msg);
                throw new KeyMgtException(msg);
            }
        } catch (io.entgra.device.mgt.core.apimgt.extension.rest.api.exceptions.BadRequestException e) {
            msg = "Error while trying to retrieve the application";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        } catch (UnexpectedResponseException e) {
            msg = "Received invalid response for the API applications retrieving REST API call.";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        } catch (APIServicesException e) {
            msg = "Error occurred while processing the API Response.";
            log.error(msg, e);
            throw new KeyMgtException(msg, e);
        }
    }

    private String createOauthAppCreationPayload(String clientName, String owner, String[] tags, int validityPeriod,
                                                 String password, List<String> supportedGrantTypes, String callbackUrl) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("applicationName", clientName);
        jsonObject.put("username", owner);
        jsonObject.put("tags", tags);
        jsonObject.put("validityPeriod", validityPeriod);
        jsonObject.put("password", password);
        jsonObject.put("supportedGrantTypes", supportedGrantTypes);
        jsonObject.put("callbackUrl", callbackUrl);
        return jsonObject.toString();
    }

    private String generateCustomKeyManagerName(String tenantDomain) {
        return KeyMgtConstants.CUSTOM_KEY_MANAGER_NAME_PREFIX + tenantDomain;
    }

    private RealmService getRealmService() {
        if(realmService == null) {
            PrivilegedCarbonContext context = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            return (RealmService) context.getOSGiService(RealmService.class, null);
        } else {
            return realmService;
        }
    }

    private static OkHttpClient getOkHttpClient() {
        X509TrustManager trustAllCerts = new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[0];
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        };
        return new OkHttpClient.Builder()
                .sslSocketFactory(getSimpleTrustedSSLSocketFactory(), trustAllCerts)
                .hostnameVerifier((hostname, sslSession) -> true).build();
    }

    private static SSLSocketFactory getSimpleTrustedSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            return null;
        }

    }

    private KeyManagerConfigurations getKeyManagerConfig() {
        if (kmConfig != null) {
            return kmConfig;
        } else {
            DeviceManagementConfig deviceManagementConfig = DeviceConfigurationManager.getInstance().getDeviceManagementConfig();
            return deviceManagementConfig.getKeyManagerConfigurations();
        }
    }
}
