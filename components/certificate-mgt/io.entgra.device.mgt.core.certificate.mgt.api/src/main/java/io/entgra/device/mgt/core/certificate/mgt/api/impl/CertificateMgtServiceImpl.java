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

package io.entgra.device.mgt.core.certificate.mgt.api.impl;

import io.entgra.device.mgt.core.certificate.mgt.api.CertificateMgtService;
import io.entgra.device.mgt.core.certificate.mgt.api.beans.ErrorResponse;
import io.entgra.device.mgt.core.certificate.mgt.api.exception.Message;
import io.entgra.device.mgt.core.certificate.mgt.api.exception.UnexpectedServerErrorException;
import io.entgra.device.mgt.core.certificate.mgt.core.exception.KeystoreException;
import io.entgra.device.mgt.core.certificate.mgt.core.impl.CertificateGenerator;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@Path("/scep")
public class CertificateMgtServiceImpl implements CertificateMgtService {
    private static Log log = LogFactory.getLog(CertificateMgtServiceImpl.class);

    @POST
    @Path("/sign-csr")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getSignedCertFromCSR(
            @HeaderParam("If-Modified-Since") String ifModifiedSince, String binarySecurityToken) {
        Message message = new Message();
        X509Certificate signedCert;
        String singedCertificate;
        Base64 base64 = new Base64();
        CertificateGenerator certificateGenerator = new CertificateGenerator();
        try {
            if (certificateGenerator.getSignedCertificateFromCSR(binarySecurityToken) == null) {
                message.setErrorMessage("Error occurred while signing the CSR.");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                        entity(message).build();
            } else {
                signedCert = certificateGenerator.getSignedCertificateFromCSR(binarySecurityToken);
                singedCertificate = base64.encodeToString(signedCert.getEncoded());
                return Response.status(Response.Status.OK).entity(singedCertificate).build();
            }
        } catch (KeystoreException e) {
            String msg = "Error occurred while fetching certificate.";
            log.error(msg, e);
            throw new UnexpectedServerErrorException(new ErrorResponse.ErrorResponseBuilder().setCode(
                    500l).setMessage(msg).build());
        } catch (CertificateEncodingException e) {
            String msg = "Error occurred while encoding the certificate.";
            log.error(msg, e);
            throw new UnexpectedServerErrorException(new ErrorResponse.ErrorResponseBuilder().setCode(
                    500l).setMessage(msg).build());
        }
    }
}
