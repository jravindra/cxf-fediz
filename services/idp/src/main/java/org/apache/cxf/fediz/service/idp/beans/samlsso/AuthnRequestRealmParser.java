/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.fediz.service.idp.beans.samlsso;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.w3c.dom.Document;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.webflow.execution.RequestContext;

/**
 * Parse the received AuthnRequest and extract the home realm of the request from the Issuer
 * value.
 */
@Component
public class AuthnRequestRealmParser {

    private static final Logger LOG = LoggerFactory.getLogger(AuthnRequestRealmParser.class);

    public String retrieveRealm(RequestContext context) {
        String samlRequest = context.getFlowScope().getString(SSOConstants.SAML_REQUEST);
        LOG.debug("Received SAML Request: {}", samlRequest);

        if (samlRequest != null) {
            try {
                AuthnRequest parsedRequest = extractRequest(samlRequest);
                if (parsedRequest.getIssuer() != null) {
                    String issuer = parsedRequest.getIssuer().getValue();
                    LOG.debug("Parsed SAML AuthnRequest Issuer: {}", issuer);
                    return issuer;
                }
            } catch (Exception ex) {
                LOG.warn("Error parsing request: {}", ex.getMessage());
                return null;
            }
        }
        
        LOG.debug("No SamlRequest available to be parsed");
        return null;
    }
    
    private AuthnRequest extractRequest(String samlRequest) throws Exception {
        byte[] deflatedToken = Base64Utility.decode(samlRequest);
        InputStream tokenStream = new DeflateEncoderDecoder().inflateToken(deflatedToken);

        Document responseDoc = StaxUtils.read(new InputStreamReader(tokenStream, "UTF-8"));
        AuthnRequest request = 
            (AuthnRequest)OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        if (LOG.isDebugEnabled()) {
            LOG.debug(DOM2Writer.nodeToString(responseDoc));
        }
        return request;
    }
}
