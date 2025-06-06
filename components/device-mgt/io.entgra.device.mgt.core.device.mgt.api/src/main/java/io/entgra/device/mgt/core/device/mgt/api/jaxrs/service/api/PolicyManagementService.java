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
package io.entgra.device.mgt.core.device.mgt.api.jaxrs.service.api;

import io.entgra.device.mgt.core.apimgt.annotations.Scope;
import io.entgra.device.mgt.core.apimgt.annotations.Scopes;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.ErrorResponse;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.PolicyWrapper;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.PriorityUpdatedPolicyWrapper;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.ProfileFeature;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.util.Constants;
import io.entgra.device.mgt.core.device.mgt.common.policy.mgt.Policy;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Policy related REST-API. This can be used to manipulated policies and associate them with devices, users, roles,
 * groups.
 */

@SwaggerDefinition(
        info = @Info(
                version = "1.0.0",
                title = "",
                extensions = {
                        @Extension(properties = {
                                @ExtensionProperty(name = "name", value = "DevicePolicyManagement"),
                                @ExtensionProperty(name = "context", value = "/api/device-mgt/v1.0/policies"),
                        })
                }
        ),
        tags = {
                @Tag(name = "device_management", description = "")
        }
)
@Scopes(
        scopes = {
                @Scope(
                        name = "Adding a Policy",
                        description = "Adding a Policy",
                        key = "pm:policies:add",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/add"}
                ),
                @Scope(
                        name = "Getting Details of Policies",
                        description = "Getting Details of Policies",
                        key = "dm:policies:view",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/view"}
                ),
                @Scope(
                        name = "Getting Details of a Policy",
                        description = "Getting Details of a Policy",
                        key = "pm:policies:details:view",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/view-details"}
                ),
                @Scope(
                        name = "Updating a Policy",
                        description = "Updating a Policy",
                        key = "pm:policies:update",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/update"}
                ),
                @Scope(
                        name = "Removing Multiple Policies",
                        description = "Removing Multiple Policies",
                        key = "pm:policies:remove",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/remove"}
                ),
                @Scope(
                        name = "Activating Policies",
                        description = "Activating Policies",
                        key = "pm:policies:activate",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/activate"}
                ),
                @Scope(
                        name = "Deactivating Policies",
                        description = "Deactivating Policies",
                        key = "pm:policies:deactivate",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/deactivate"}
                ),
                @Scope(
                        name = "Applying Changes on Policies",
                        description = "Applying Changes on Policies",
                        key = "pm:policies:change",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/apply-changes"}
                ),
                @Scope(
                        name = "Updating the Policy Priorities",
                        description = "Updating the Policy Priorities",
                        key = "pm:policies:priorities:update",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/update-priority"}
                ),
                @Scope(
                        name = "Fetching the Effective Policy",
                        description = "Fetching the Effective Policy",
                        key = "pm:policies:effective-policy",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/policies/view-effective-policy"}
                )
        }
)
@Api(value = "Device Policy Management", description = "This API includes the functionality around device policy " +
        "management")
@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PolicyManagementService {

    @POST
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "POST",
            value = "Adding a Policy",
            notes = "Add a policy using this REST API command. When adding a policy you will have the option of " +
                    "saving the policy or saving and publishing the policy." +
                    "Using this REST API you are able to save a created Policy and this policy will be in the " +
                    "inactive state.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:add")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 201,
                            message = "Created. \n Successfully created the policy.",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Location",
                                            description = "The URL of the added policy."),
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests.")
                            }
                    ),
                    @ApiResponse(
                            code = 303,
                            message = "See Other. \n The source can be retrieved from the URL specified in the " +
                                    "location header",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Location",
                                            description = "The Source URL of the document.")}),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 401,
                            message = "Not Found. \n The user that is currently logged in is not authorized to add " +
                                    "policies.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 415,
                            message = "Unsupported media type. \n The format of the requested entity was not " +
                                    "supported."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n " +
                                    "Server error occurred while adding a new policy.",
                            response = ErrorResponse.class)
            })
    Response addPolicy(
            @ApiParam(
                    name = "policy",
                    value = "The properties required to add a new policy.",
                    required = true)
                    @Valid PolicyWrapper policy);

    @POST
    @Path("/validate")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "POST",
            value = "Validate a policy",
            notes = "Validate a policy",
            tags = "Device Policy Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:add")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully validated the policy.",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Location",
                                            description = "The URL of the added policy."),
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests.")
                            }
                    ),
                    @ApiResponse(
                            code = 303,
                            message = "See Other. \n The source can be retrieved from the URL specified in the " +
                                    "location header",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Location",
                                            description = "The Source URL of the document.")}),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 401,
                            message = "Not Found. \n The user that is currently logged in is not " +
                                    "authorized to validate policies.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 415,
                            message = "Unsupported media type. \n The format of the requested entity was not " +
                                    "supported."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n " +
                                    "Server error occurred while validating a policy.",
                            response = ErrorResponse.class)
            })
    Response validatePolicy(@ApiParam(
            name = "profileFeaturesList",
            value = "The properties required to validate a policy.",
            required = true) List<ProfileFeature> profileFeaturesList);

    @GET
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting Details of Policies",
            responseContainer = "List",
            notes = "Retrieve the details of all the policies in WSO2 EMM.",
            response = Policy.class,
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "dm:policies:view")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched policies.",
                            response = Policy.class,
                            responseContainer = "List",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests."),
                            }
                    ),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                    "of the requested resource."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported"),
                    @ApiResponse(
                            code = 500,
                            message = ("Internal Server Error. \n Server error occurred while fetching the policies."),
                            response = ErrorResponse.class)
            })
    Response getPolicies(
            @ApiParam(
                    name = "If-Modified-Since",
                    value = "Checks if the requested variant was modified, since the specified date-time. \n" +
                            "Provide the value in the following format: EEE, d MMM yyyy HH:mm:ss Z.\n" +
                            "Example: Mon, 05 Jan 2014 15:10:00 +0200",
                    required = false)
            @HeaderParam("If-Modified-Since")
                    String ifModifiedSince,
            @ApiParam(
                    name = "offset",
                    value = "The starting pagination index for the complete list of qualified items",
                    required = false,
                    defaultValue = "0")
            @QueryParam("offset")
                    int offset,
            @ApiParam(
                    name = "limit",
                    value = "Provide how many policy details you require from the starting pagination index/offset.",
                    required = false,
                    defaultValue = "5")
            @QueryParam("limit")
                    int limit);

    @GET
    @Path("/{id}")
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting Details of a Policy",
            notes = "Retrieve the details of a policy that is in WSO2 EMM.",
            response = Policy.class,
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:details:view")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched the policy.",
                            response = Policy.class,
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests."),
                            }
                    ),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                    "of the requested resource.\n"),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n A specified policy was not found.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n Server error occurred while fetching the " +
                                    "policy.",
                            response = ErrorResponse.class)
            })
    Response getPolicy(
            @ApiParam(
                    name = "id",
                    value = "The policy identifier.",
                    required = true,
                    defaultValue = "")
            @PathParam("id")
                    int id,
            @ApiParam(
                    name = "If-Modified-Since",
                    value = "Checks if the requested variant was modified, since the specified date-time. \n" +
                            "Provide the value in the following format: EEE, d MMM yyyy HH:mm:ss Z.\n" +
                            "Example: Mon, 05 Jan 2014 15:10:00 +0200",
                    required = false)
            @HeaderParam("If-Modified-Since")
                    String ifModifiedSince);

    @PUT
    @Path("/{id}")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "PUT",
            value = "Updating a Policy",
            notes = "Make changes to an existing policy by updating the policy using this resource.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:update")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully updated the policy.",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Location",
                                            description = "The URL of the updated device."),
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests.")
                            }
                    ),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n The specified resource does not exist.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 415,
                            message = "Unsupported media type. \n The format of the requested entity was not " +
                                    "supported."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n " +
                                    "Server error occurred while updating the policy.",
                            response = ErrorResponse.class)
            })
    Response updatePolicy(
            @ApiParam(
                    name = "id",
                    value = "The policy ID.",
                    required = true,
                    defaultValue = "1")
            @PathParam("id")
                    int id,
            @ApiParam(
                    name = "policy",
                    value = "Update the required property details.",
                    required = true)
                    @Valid PolicyWrapper policy);

    @POST
    @Path("/remove-policy")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "POST",
            value = "Removing Multiple Policies",
            notes = "Delete one or more than one policy using this API.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:remove")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully removed the policy."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n The specified resource does not exist.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 415,
                            message = "Unsupported media type. \n The format of the requested entity was not " +
                                    "supported.\n "
                                    + "supported format."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n " +
                                    "Server error occurred whilst bulk removing policies.",
                            response = ErrorResponse.class)
            })
    Response removePolicies(
            @ApiParam(
                    name = "policyIds",
                    value = "The list of policy IDs to be removed.",
                    required = true,
                    defaultValue = "[1]")
                    List<Integer> policyIds);

    @POST
    @Path("/activate-policy")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "POST",
            value = "Activating Policies",
            notes = "Publish a policy using this API to bring a policy that is in the inactive state to the active " +
                    "state.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:activate")
                })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "Successfully activated the policy."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n The specified resource/s does not exist.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 500,
                            message = "Sever error whilst activating the policies.",
                            response = ErrorResponse.class)
            })
    Response activatePolicies(
            @ApiParam(
                    name = "policyIds",
                    value = "The list of the policy IDs to be activated",
                    required = true,
                    defaultValue = "[1]")
                    List<Integer> policyIds);

    @POST
    @Path("/deactivate-policy")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "POST",
            value = "Deactivating Policies",
            notes = "Unpublish a policy using this API to bring a policy that is in the active state to the inactive " +
                    "state.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:deactivate")
                })
            }
    )
    @ApiResponses(
            value = {
            @ApiResponse(
                    code = 200,
                    message = "Successfully deactivated the policy."),
            @ApiResponse(
                    code = 400,
                    message = "Bad Request. \n Invalid request or validation error.",
                    response = ErrorResponse.class),
            @ApiResponse(
                    code = 404,
                    message = "Not Found. \n The specified resource does not exist.",
                    response = ErrorResponse.class),
            @ApiResponse(
                    code = 500,
                    message = "ErrorResponse in deactivating policies.",
                    response = ErrorResponse.class)
    })
    Response deactivatePolicies(
            @ApiParam(
                    name = "policyIds",
                    value = "The list of Policy IDs that needs to be deactivated.",
                    required = true,
                    defaultValue = "[1]")
                    List<Integer> policyIds);

    @PUT
    @Produces("application/json")
    @Path("apply-changes")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "PUT",
            value = "Applying Changes on Policies",
            notes = "Policies in the active state will be applied to new devices that register with WSO2 EMM based on" +
                    " the policy enforcement criteria . In a situation where you need to make changes to existing" +
                    " policies (removing, activating, deactivating and updating) or add new policies, the existing" +
                    " devices will not receive these changes immediately. Once all the required changes are made" +
                    " you need to apply the changes to push the policy changes to the existing devices.",
            tags = "Device Policy Management",
            extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:change")
                })
            }
    )
    @ApiResponses(
            value = {
            @ApiResponse(
                    code = 200,
                    message = "Successfully updated the EMM server with the policy changes."),
            @ApiResponse(
                    code = 500,
                    message = "ErrorResponse in deactivating policies.",
                    response = ErrorResponse.class)
    })
    Response applyChanges();


    @PUT
    @Path("/priorities")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "PUT",
            value = "Updating the Policy Priorities",
            notes = "Make changes to the existing policy priority order by updating the priority order using this API.",
            tags = "Device Policy Management",
            extensions = {
            @Extension(properties = {
                    @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:priorities:update")
            })
    }
    )
    @ApiResponses(
            value = {
            @ApiResponse(
                    code = 200,
                    message = "Successfully updated the policy priority order."),
            @ApiResponse(
                    code = 400,
                    message = "Bad Request. Did not update the policy priority order.",
                    response = ErrorResponse.class),
            @ApiResponse(
                    code = 500,
                    message = "Exception in updating the policy priorities.",
                    response = ErrorResponse.class)
    })
    Response updatePolicyPriorities(
            @ApiParam(
                    name = "priorityUpdatedPolicies",
                    value = "List of policies with priorities",
                    required = true)
                    List<PriorityUpdatedPolicyWrapper> priorityUpdatedPolicies);

    @GET
    @Path("/effective-policy/{deviceType}/{deviceId}")
    @ApiOperation(
            consumes = MediaType.APPLICATION_JSON,
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting the Effective Policy",
            notes = "Retrieve the effective policy of a device using this API.",
            tags = "Device Policy Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "pm:policies:effective-policy")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched the policy.",
                            response = Policy.class,
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests."),
                            }
                    ),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                    "of the requested resource.\n"),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n A specified policy was not found.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported."),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. \n Server error occurred while fetching the " +
                                    "policy.",
                            response = ErrorResponse.class)
            })
    Response getEffectivePolicy(
            @ApiParam(
                    name = "deviceType",
                    value = "The device type name, such as ios, android, windows or fire-alarm.",
                    required = true)
            @PathParam("deviceType")
            @Size(max = 45)
                    String deviceType,
            @ApiParam(
                    name = "deviceId",
                    value = "The device identifier of the device you want ot get details.",
                    required = true)
            @PathParam("deviceId")
            @Size(max = 45)
                    String deviceId);

    @GET
    @Path("/type/{policyType}")
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting Details of Policies",
            responseContainer = "List",
            notes = "Retrieve the details of all the policies filtered by policy type in Entgra " +
                    "EMM.",
            response = Policy.class,
            tags = "Device Policy Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "dm:policies:view")
                    })
            },
            nickname = "getPoliciesFilteredByType"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched policies.",
                            response = Policy.class,
                            responseContainer = "List",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                          "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                          "Used by caches, or in conditional requests."),
                            }
                    ),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                      "of the requested resource."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported"),
                    @ApiResponse(
                            code = 500,
                            message = ("Internal Server Error. \n Server error occurred while fetching the policies."),
                            response = ErrorResponse.class)
            })
    Response getPolicies(@ApiParam(
            name = "policyType",
            value = "The policy type, such as general policy, Geo policy.",
            required = true)
                         @PathParam("policyType")
                                 String policyType,
                         @ApiParam(
                                 name = "If-Modified-Since",
                                 value = "Checks if the requested variant was modified, since the specified date-time. \n" +
                                         "Provide the value in the following format: EEE, d MMM yyyy HH:mm:ss Z.\n" +
                                         "Example: Mon, 05 Jan 2014 15:10:00 +0200",
                                 required = false)
                         @HeaderParam("If-Modified-Since")
                                 String ifModifiedSince,
                         @ApiParam(
                                 name = "offset",
                                 value = "The starting pagination index for the complete list of qualified items",
                                 required = false,
                                 defaultValue = "0")
                         @QueryParam("offset")
                                 int offset,
                         @ApiParam(
                                 name = "limit",
                                 value = "Provide how many policy details you require from the starting pagination index/offset.",
                                 required = false,
                                 defaultValue = "5")
                         @QueryParam("limit")
                                 int limit
    );

    @GET
    @Path("/list")
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting list of Policies",
            responseContainer = "List",
            notes = "Retrieve the details of all the policies in WSO2 EMM.",
            response = Policy.class,
            tags = "Device Policy Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "dm:policies:view")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched policies.",
                            response = Policy.class,
                            responseContainer = "List",
                            responseHeaders = {
                                    @ResponseHeader(
                                            name = "Content-Type",
                                            description = "The content type of the body"),
                                    @ResponseHeader(
                                            name = "ETag",
                                            description = "Entity Tag of the response resource.\n" +
                                                    "Used by caches, or in conditional requests."),
                                    @ResponseHeader(
                                            name = "Last-Modified",
                                            description = "Date and time the resource was last modified.\n" +
                                                    "Used by caches, or in conditional requests."),
                            }
                    ),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                    "of the requested resource."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid request or validation error.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported"),
                    @ApiResponse(
                            code = 500,
                            message = ("Internal Server Error. \n Server error occurred while fetching the policies."),
                            response = ErrorResponse.class)
            })
    Response getPolicyList(
            @ApiParam(
                    name = "name",
                    value = "The name of the policy that needs filtering.",
                    required = false)
            @QueryParam("name")
                    String name,
            @ApiParam(
                    name = "type",
                    value = "The type of the policy that needs filtering.",
                    required = false)
            @QueryParam("type")
                    String type,
            @ApiParam(
                    name = "status",
                    value = "The status of the policy that needs filtering.",
                    required = false)
            @QueryParam("status")
                    String status,
            @ApiParam(
                    name = "deviceType",
                    value = "The device type of the policy that needs filtering.",
                    required = false)
            @QueryParam("deviceType")
            String deviceType,
            @ApiParam(
                    name = "If-Modified-Since",
                    value = "Checks if the requested variant was modified, since the specified date-time. \n" +
                            "Provide the value in the following format: EEE, d MMM yyyy HH:mm:ss Z.\n" +
                            "Example: Mon, 05 Jan 2014 15:10:00 +0200",
                    required = false)
            @HeaderParam("If-Modified-Since")
            String ifModifiedSince,
            @ApiParam(
                    name = "offset",
                    value = "The starting pagination index for the complete list of qualified items",
                    required = false,
                    defaultValue = "0")
            @QueryParam("offset")
            int offset,
            @ApiParam(
                    name = "limit",
                    value = "Provide how many policy details you require from the starting pagination index/offset.",
                    required = false,
                    defaultValue = "5")
            @QueryParam("limit")
            int limit);
}
