/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.analytics.service.impl.google;

public class GoogleCreds
{
    private String private_key_id;
    private String private_key;
    private String client_email;
    private String client_id;
    private String type;
	private String auth_uri;
    private String token_uri;
    private String auth_provider_x509_cert_url;
    private String client_x509_cert_url;
    private String project_id;
    
    public String getProject_id() {
		return project_id;
	}
	public void setProject_id(String project_id) {
		this.project_id = project_id;
	}
	public String getAuth_uri() {
		return auth_uri;
	}
	public void setAuth_uri(String auth_uri) {
		this.auth_uri = auth_uri;
	}
	public String getToken_uri() {
		return token_uri;
	}
	public void setToken_uri(String token_uri) {
		this.token_uri = token_uri;
	}
	public String getAuth_provider_x509_cert_url() {
		return auth_provider_x509_cert_url;
	}
	public void setAuth_provider_x509_cert_url(String auth_provider_x509_cert_url) {
		this.auth_provider_x509_cert_url = auth_provider_x509_cert_url;
	}
	public String getClient_x509_cert_url() {
		return client_x509_cert_url;
	}
	public void setClient_x509_cert_url(String client_x509_cert_url) {
		this.client_x509_cert_url = client_x509_cert_url;
	}
	
    public String getPrivate_key_id()
    {
        return private_key_id;
    }
    public void setPrivate_key_id(String private_key_id)
    {
        this.private_key_id = private_key_id;
    }
    public String getPrivate_key()
    {
        return private_key;
    }
    public void setPrivate_key(String private_key)
    {
        this.private_key = private_key;
    }
    public String getClient_email()
    {
        return client_email;
    }
    public void setClient_email(String client_email)
    {
        this.client_email = client_email;
    }
    public String getClient_id()
    {
        return client_id;
    }
    public void setClient_id(String client_id)
    {
        this.client_id = client_id;
    }
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
}
