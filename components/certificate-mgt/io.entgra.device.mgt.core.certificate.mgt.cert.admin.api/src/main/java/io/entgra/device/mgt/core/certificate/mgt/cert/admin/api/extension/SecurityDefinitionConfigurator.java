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
package io.entgra.device.mgt.core.certificate.mgt.cert.admin.api.extension;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;

import java.util.HashMap;
import java.util.Map;

@SwaggerDefinition(
        basePath = "/api/certificate-mgt/v1.0",
        host = "localhost:9443"
)
public class SecurityDefinitionConfigurator implements ReaderListener {

    public static final String TOKEN_AUTH_SCHEME = "swagger_auth";

    @Override
    public void beforeScan(Reader reader, Swagger swagger) {

    }

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        OAuth2Definition tokenScheme = new OAuth2Definition();
        tokenScheme.setType("oauth2");
        tokenScheme.setFlow("application");
        tokenScheme.setTokenUrl("https://" + swagger.getHost() + "/oauth2/token");
        tokenScheme.setAuthorizationUrl("https://" + swagger.getHost() + "/oauth2/authorize");
        tokenScheme.addScope("write:everything", "Full access");

        Map<String, SecuritySchemeDefinition> schemes = new HashMap<>();
        schemes.put(TOKEN_AUTH_SCHEME, tokenScheme);

        swagger.setSecurityDefinitions(schemes);
    }

}

