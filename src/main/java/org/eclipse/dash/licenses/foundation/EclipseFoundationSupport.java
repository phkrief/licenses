/*************************************************************************
 * Copyright (c) 2019, The Eclipse Foundation and others.
 * 
 * This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License 2.0 which accompanies this 
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.dash.licenses.IContentData;
import org.eclipse.dash.licenses.IContentId;
import org.eclipse.dash.licenses.ILicenseDataProvider;
import org.eclipse.dash.licenses.ISettings;

public class EclipseFoundationSupport implements ILicenseDataProvider {

	private ISettings settings;

	public EclipseFoundationSupport(ISettings settings) {
		this.settings = settings;
	}

	@Override
	public void queryLicenseData(Collection<IContentId> ids, Consumer<IContentData> consumer) {
		if (ids.size() == 0)
			return;

		String url = settings.getLicenseCheckUrl();

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			JsonArrayBuilder builder = Json.createBuilderFactory(null).createArrayBuilder();
			ids.stream().forEach(id -> builder.add(id.toString()));
			String json = builder.build().toString();

			HttpPost post = new HttpPost(url);
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			parameters.add(new BasicNameValuePair("json", json));

			post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));

			CloseableHttpResponse response = httpclient.execute(post);
			if (response.getStatusLine().getStatusCode() == 200) {
				InputStream content = response.getEntity().getContent();
				JsonReader reader = Json.createReader(new InputStreamReader(content, "UTF-8"));
				JsonObject read = (JsonObject) reader.read();

				JsonObject approved = read.getJsonObject("approved");
				if (approved != null)
					approved.forEach((key, each) -> consumer.accept(new FoundationData(each.asJsonObject())));

				JsonObject restricted = read.getJsonObject("restricted");
				if (restricted != null)
					restricted.forEach((key, each) -> consumer.accept(new FoundationData(each.asJsonObject())));

				content.close();
			}
			response.close();
		} catch (IOException e) {
			// FIXME Handle gradefuly
			throw new RuntimeException(e);
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
