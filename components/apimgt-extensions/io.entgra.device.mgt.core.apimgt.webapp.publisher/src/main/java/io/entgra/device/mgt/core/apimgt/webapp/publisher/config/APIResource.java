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

package io.entgra.device.mgt.core.apimgt.webapp.publisher.config;

import io.entgra.device.mgt.core.apimgt.webapp.publisher.dto.ApiScope;

public class APIResource {

    private String AuthType;
    private String HttpVerb;
    private String Uri;
    private String UriTemplate;
    private String consumes;
    private String produces;
    private ApiScope scope;
    private String uriMapping;

    public String getAuthType() {
        return AuthType;
    }

    public void setAuthType(String authType) {
        AuthType = authType;
    }

    public String getHttpVerb() {
        return HttpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        HttpVerb = httpVerb;
    }

    public String getUri() {
        return Uri;
    }

    public void setUri(String uri) {
        Uri = uri;
    }

    public String getUriTemplate() {
        return UriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        UriTemplate = uriTemplate;
    }

    public String getConsumes() {
        return consumes;
    }

    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    public String getProduces() {
        return produces;
    }

    public void setProduces(String produces) {
        this.produces = produces;
    }

    public ApiScope getScope() {
        return scope;
    }

    public void setScope(ApiScope scope) {
        this.scope = scope;
    }

    public String getUriMapping() {
        return uriMapping;
    }

    public void setUriMapping(String uriMapping) {
        this.uriMapping = uriMapping;
    }
}
