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
package io.entgra.device.mgt.core.device.mgt.core;

import io.entgra.device.mgt.core.device.mgt.core.operation.mgt.OperationMgtConstants;
import io.entgra.device.mgt.core.device.mgt.core.operation.mgt.PolicyOperation;
import org.wso2.carbon.user.api.Permission;

public final class DeviceManagementConstants {

    public static final String DM_CACHE_MANAGER = "DM_CACHE_MANAGER";
    public static final String DEVICE_CACHE = "DEVICE_CACHE";
    public static final String API_RESOURCE_PERMISSION_CACHE = "API_RESOURCE_CACHE_CACHE";
    public static final String GEOFENCE_CACHE = "GEOFENCE_CACHE";
    public static final String BILLING_CACHE = "BILLING_CACHE";
    public static final String META_KEY = "PER_DEVICE_COST";
    public static final String DEP_META_KEY = "DEP_CONFIG";
    public static final String ACTIVE_STATUS = "ACTIVE";
    public static final String ENROLLMENT_NOTIFICATION_API_ENDPOINT = "/api/device-mgt/enrollment-notification";
    public static final String URL_SEPERATOR = "/";

    public static final class ConfigurationManagement {
        private ConfigurationManagement(){
            throw new AssertionError();
        }
        public static final String SCOPES_FOR_TOKEN = "dm:devices:ops:view dm:device:event:publish win:devices:enroll";
        public static final String IOT_GATEWAY_HOST = "iot.gateway.host";
        public static final String IOT_GATEWAY_HTTPS_PORT = "iot.gateway.https.port";
        public static final String IOT_CORE_HOST = "iot.core.host";
        public static final String IOT_CORE_HTTPS_PORT = "iot.core.https.port";
        public static final String APPLICATION_REGISTRATION_API_ENDPOINT =
                "/api-application-registration/register";
        public static final String AUTHORIZATION_HEADER = "authorization";
        public static final String BASIC_AUTH = "Basic";

    }
    public static final class Common {
        private Common() {
            throw new AssertionError();
        }

        public static final String SETUP_PROPERTY = "setup";
        public static final String DEFAULT_LICENSE_CONFIG_XML_NAME = "license-config.xml";
        public static final String SERIAL = "SERIAL";
        public static final String ENROLLMENT_CONFIGURATION = "enrollmentConfiguration";
    }

	public static final class AppManagement {
		private AppManagement() {
			throw new AssertionError();
		}

		public static final String OAUTH_APPLICATION_NAME = "app_management_application";
		public static final String OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
		public final static String OAUTH_VERSION_2 = "oauth-2.0";
		public final static String OAUTH_ADMIN_SERVICE = "/services/OAuthAdminService";
	}

    public static final class EmailNotifications {
        private EmailNotifications() {
            throw new AssertionError();
        }

        public static final String ENROL_NOTIFICATION_TYPE = "enrol";
        public static final String USER_REGISTRATION_NOTIFICATION_TYPE = "userRegistration";
    }

    public static final class AuthorizationSkippedOperationCodes {
        private AuthorizationSkippedOperationCodes() {
            throw new AssertionError();
        }

        public static final String MONITOR_OPERATION_CODE = "MONITOR";
        public static final String POLICY_OPERATION_CODE = PolicyOperation.POLICY_OPERATION_CODE;
        public static final String POLICY_REVOKE_OPERATION_CODE = OperationMgtConstants.OperationCodes.POLICY_REVOKE;
        public static final String EVENT_CONFIG_OPERATION_CODE = OperationMgtConstants.OperationCodes.EVENT_CONFIG;
        public static final String EVENT_UPDATE_OPERATION_CODE = OperationMgtConstants.OperationCodes.EVENT_UPDATE;
        public static final String EVENT_REVOKE_OPERATION_CODE = OperationMgtConstants.OperationCodes.EVENT_REVOKE;
    }

    public static final class CorrectiveActions {
        private CorrectiveActions() {throw new AssertionError();}

        public static final String E_MAIL = "MAIL";
    }

    public static final class EmailAttributes {
        private EmailAttributes() {
            throw new AssertionError();
        }

        public static final String ENCODED_SCHEME = "UTF-8";
        public static final String FIRST_NAME = "first-name";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String DOMAIN = "domain-name";

        public static final String SERVER_BASE_URL_HTTPS = "base-url-https";
        public static final String SERVER_BASE_URL_HTTP = "base-url-http";
        public static final String DOWNLOAD_URL = "download-url";

        public static final String USER_REGISTRATION_TEMPLATE = "user-registration";
        public static final String USER_ENROLLMENT_TEMPLATE = "user-enrollment";
        public static final String USER_VERIFY_TEMPLATE = "user-verify";
        public static final String PRODUCT_DOWNLOAD_LINK_SHARING_TEMPLATE = "share-product-download-url";
        public static final String POLICY_VIOLATE_TEMPLATE = "policy-violating-notifier";
        public static final String USER_WELCOME_TEMPLATE = "user-welcome";
        public static final String DEFAULT_ENROLLMENT_TEMPLATE = "default-enrollment-invitation";
        public static final String ENROLLMENT_GUIDE_TEMPLATE = "enrollment-guide";
        public static final String DEVICE_ENROLLMENT_MAIL_KEY = "enrollment";
        public static final String TEMPLATE_NAME_PART_JOINER = "-";
        public static final String ENROLLMENT_TYPE_SPLITTER = "_";
        public static final String DOC_URL = "doc-url";
    }

    public static final class OperationAttributes {
        private OperationAttributes() {throw new AssertionError(); }
        public static final String ACTIVITY = "ACTIVITY_";
        public static final int APPLIST_VERSION_MAX_LENGTH = 50;
    }

    public static final class PushNotifications {
        private PushNotifications() {
            throw new AssertionError();
        }
        public static final int DEFAULT_SCHEDULER_TASK_INITIAL_DELAY = 60000;
        public static final int DEFAULT_BATCH_DELAY_MILLS = 60000;
        public static final int DEFAULT_BATCH_SIZE = 1000;
    }

    public static final class User {
        private User() {
            throw new AssertionError();
        }

        public static final String DEFAULT_DEVICE_USER = "Internal/devicemgt-user";
        public static final String DEFAULT_DEVICE_ADMIN = "Internal/devicemgt-admin";
        public static final String DEFAULT_UI_EXECUTER = "Internal/ui-executer";

        public static final String CLAIM_EMAIL_ADDRESS = "http://wso2.org/claims/emailaddress";
        public static final String CLAIM_FIRST_NAME = "http://wso2.org/claims/givenname";

        // Permissions that are given for a normal device user.
        public static final Permission[] PERMISSIONS_FOR_DEVICE_USER = {
                new Permission("/permission/admin/Login", "ui.execute"),
                new Permission("/permission/admin/device-mgt/device/api/subscribe", "ui.execute"),
                new Permission("/permission/admin/device-mgt/devices/enroll", "ui.execute"),
                new Permission("/permission/admin/device-mgt/devices/disenroll", "ui.execute"),
                new Permission("/permission/admin/device-mgt/devices/owning-device/view", "ui.execute"),
                new Permission("/permission/admin/device-mgt/devices/owning-device/operations/view", "ui.execute"),
                new Permission("/permission/admin/device-mgt/metadata", "ui.execute"),
                new Permission("/permission/admin/manage/portal", "ui.execute")
        };

        // Permissions that are given for a normal device admin.
        public static final Permission[] PERMISSIONS_FOR_DEVICE_ADMIN = {
                new Permission("/permission/admin/device-mgt", "ui.execute"),
                new Permission("/permission/admin/manage/api", "ui.execute"),
                new Permission("/permission/admin/manage/portal", "ui.execute")
        };
    }

    public static final class Report {
        private Report() {
            throw new AssertionError();
        }
        public static final String REPORTING_EVENT_HOST = "iot.reporting.event.host";
        public static final String REPORTING_CONTEXT = "/reporting/api/analyticsadmin/v1.0/event";
        public static final String DEVICE_INFO_PARAM =  "device-info";
        public static final String APP_USAGE_ENDPOINT = REPORTING_CONTEXT + "/app-usage";
    }

    public static final class Payload {
        private Payload() {
            throw new AssertionError();
        }
        public static final String DEVICE_INFO_DEVICE_NAME = "DEVICE_NAME";
        public static final String DEVICE_INFO_IMEI = "IMEI";
        public static final String DEVICE_INFO_IMSI =  "IMSI";;
    }
}
