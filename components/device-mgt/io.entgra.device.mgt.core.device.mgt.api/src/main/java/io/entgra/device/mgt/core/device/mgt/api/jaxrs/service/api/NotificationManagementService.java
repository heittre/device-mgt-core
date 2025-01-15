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
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.NotificationList;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.beans.ErrorResponse;
import io.entgra.device.mgt.core.device.mgt.api.jaxrs.util.Constants;
import io.entgra.device.mgt.core.device.mgt.common.notification.mgt.Notification;
import io.swagger.annotations.*;

import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Notifications related REST-API.
 */

@SwaggerDefinition(
        info = @Info(
                version = "1.0.0",
                title = "",
                extensions = {
                        @Extension(properties = {
                                @ExtensionProperty(name = "name", value = "DeviceNotificationManagement"),
                                @ExtensionProperty(name = "context", value = "/api/device-mgt/v1.0/notifications"),
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
                        name = "Getting All Device Notification Details",
                        description = "Getting All Device Notification Details",
                        key = "dm:notifications:view",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/notifications/view"}
                ),
                @Scope(
                        name = "Updating the Device Notification Status",
                        description = "Updating the Device Notification Status",
                        key = "dm:notif:mark-checked",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/notifications/update"}
                ),
                @Scope(
                        name = "Streaming Device Notifications",
                        description = "Real-time streaming of device notifications",
                        key = "dm:notifications:stream",
                        roles = {"Internal/devicemgt-user"},
                        permissions = {"/device-mgt/notifications/stream"}
                )
        }
)
@Api(value = "Device Notification Management", description = "Device notification related operations can be found here.")
@Path("/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface NotificationManagementService {

    @GET
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "GET",
            value = "Getting All Device Notification Details",
            notes = "Get the details of all the notifications that were pushed to the devices registered with WSO2 EMM using this REST API.",
            tags = "Device Notification Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "dm:notifications:view")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK. \n Successfully fetched the list of notifications.",
                            response = NotificationList.class,
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
                            }),
                    @ApiResponse(
                            code = 304,
                            message = "Not Modified. \n Empty body because the client already has the latest version " +
                                    "of the requested resource."),
                    @ApiResponse(
                            code = 400,
                            message = "Bad Request. \n Invalid notification status type received. \n" +
                                    "Valid status types are NEW | CHECKED",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 404,
                            message = "Not Found. \n There are no notification.",
                            response = ErrorResponse.class),
                    @ApiResponse(
                            code = 406,
                            message = "Not Acceptable.\n The requested media type is not supported"),
                    @ApiResponse(
                            code = 500,
                            message = "Internal Server Error. " +
                                    "\n Server error occurred while fetching the notification list.",
                            response = ErrorResponse.class)
            })
    Response getNotifications(
            @ApiParam(
                    name = "status",
                    value = "The status of the notification. Provide any of the following values: \n" +
                            " - NEW: Will keep the message in the unread state.\n" +
                            " - CHECKED: Will keep the message in the read state.",
                    allowableValues = "NEW, CHECKED",
                    required = false)
            @QueryParam("status") @Size(max = 45)
                    String status,
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
                    value = "The starting pagination index for the complete list of qualified items.",
                    required = false,
                    defaultValue = "0")
            @QueryParam("offset")
                    int offset,
            @ApiParam(
                    name = "limit",
                    value = "Provide how many notification details you require from the starting pagination index/offset.",
                    required = false,
                    defaultValue = "5")
            @QueryParam("limit")
                    int limit);

    @PUT
    @Path("/{id}/mark-checked")
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "PUT",
            value = "Updating the Device Notification Status",
            notes = "When a user has read the the device notification the device notification status must "
                    + "change from NEW to CHECKED. This API is used to update device notification status.",
            tags = "Device Notification Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "dm:notif:mark-checked")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK",
                            response = Notification.class),
                    @ApiResponse(
                            code = 200,
                            message = "Notification updated successfully. But the retrial of the updated "
                                    + "notification failed.",
                            response = Notification.class),
                    @ApiResponse(
                            code = 500,
                            message = "Error occurred while updating notification status.")
            }
    )
    Response updateNotificationStatus(
            @ApiParam(
                    name = "id",
                    value = "The notification ID.",
                    required = true,
                    defaultValue = "1")
            @PathParam("id") int id);


    @PUT
    @Path("/clear-all")
    @ApiOperation(
            produces = MediaType.APPLICATION_JSON,
            httpMethod = "PUT",
            value = "Clearing All Notifications",
            notes = "When a user needs to mark all the notifications as checked/read this " +
                    "function can be used to clear all notifications.",
            tags = "Device Notification Management",
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = Constants.SCOPE, value = "dm:notif:mark-checked")
                    })
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            code = 200,
                            message = "OK"),
                    @ApiResponse(
                            code = 500,
                            message = "Error occurred while clearing notifications.")
            }
    )
    Response clearAllNotifications();

    /**
     * SSE endpoint to send real-time notifications to the client.
     * @return StreamingOutput for SSE response.
     */
    @GET
    @Path("/stream")
    @Produces("text/event-stream")
    @ApiOperation(
            value = "Stream Real-Time Notifications",
            notes = "Streams real-time notifications to the client via Server-Sent Events.",
            response = StreamingOutput.class,
            extensions = {
                    @Extension(properties = {
                            @ExtensionProperty(name = "scope", value = "dm:notifications:stream")
                    })
            }
    )
    default Response streamNotifications() {
        StreamingOutput streamingOutput = new StreamingOutput() {
            public void write(OutputStream output) throws IOException {
                String notification = "data: {\"message\": \"New Notification\"}\n\n";
                while (true) {
                    try {
                        System.out.println("Sending the notification: " + notification);
                        output.write(notification.getBytes(StandardCharsets.UTF_8));
                        output.flush();

                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        };

        return Response.ok(streamingOutput)
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .header("Content-Type", "text/event-stream;charset=UTF-8")
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }
}

