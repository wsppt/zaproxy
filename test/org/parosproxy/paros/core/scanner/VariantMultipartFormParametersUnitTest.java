/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2017 The ZAP Development Team
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.parosproxy.paros.core.scanner;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMalformedHeaderException;
import org.parosproxy.paros.network.HttpMessage;

/**
 * Unit test for {@link VariantMultipartFormParameters}.
 */
public class VariantMultipartFormParametersUnitTest {
	
	private static final String CRLF = "\r\n";
	private static final String DEFAULT_PARAM_CONTENT = "anonymous" + CRLF + "anonymous2";
	private static final String DEFAULT_FILE_NAME = "file.ext";
	private static final String DEFAULT_CONTENT_TYPE = "text/plain";
	private static final String DEFAULT_FILE_PARAM_CONTENT = "contents of the file";

	@Test
	public void shouldHaveParametersListEmptyByDefault() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		// When
		List<NameValuePair> parameters = variant.getParamList();
		// Then
		assertThat(parameters, is(empty()));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldNotAllowToModifyReturnedParametersList() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		// When
		variant.getParamList().add(new NameValuePair(NameValuePair.TYPE_MULTIPART_DATA_PARAM, "name", "fred", 1));
		// Then = UnsupportedOperationException
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailToExtractParametersFromUndefinedMessage() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		HttpMessage undefinedMessage = null;
		// When
		variant.setMessage(undefinedMessage);
		// Then = IllegalArgumentException
	}

	@Test
	public void shouldExtractParametersFromAllParts() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		// When
		variant.setMessage(createMessage());
		// Then
		assertThat(variant.getParamList().size(), is(equalTo(4)));
		assertThat(variant.getParamList().get(0).getName(), is(equalTo("person")));
		assertThat(variant.getParamList().get(0).getValue(), is(equalTo(DEFAULT_PARAM_CONTENT)));
		assertThat(variant.getParamList().get(0).getType(), is(equalTo(NameValuePair.TYPE_MULTIPART_DATA_PARAM)));
		assertThat(variant.getParamList().get(1).getName(), is(equalTo("somefile")));
		assertThat(variant.getParamList().get(1).getValue(), is(equalTo(DEFAULT_FILE_PARAM_CONTENT)));
		assertThat(variant.getParamList().get(1).getType(), is(equalTo(NameValuePair.TYPE_MULTIPART_DATA_FILE_PARAM)));
		assertThat(variant.getParamList().get(2).getName(), is(equalTo("somefile")));
		assertThat(variant.getParamList().get(2).getValue(), is(equalTo(DEFAULT_FILE_NAME)));
		assertThat(variant.getParamList().get(2).getType(), is(equalTo(NameValuePair.TYPE_MULTIPART_DATA_FILE_NAME)));
		assertThat(variant.getParamList().get(3).getName(), is(equalTo("somefile")));
		assertThat(variant.getParamList().get(3).getValue(), is(equalTo(DEFAULT_CONTENT_TYPE)));
		assertThat(variant.getParamList().get(3).getType(), is(equalTo(NameValuePair.TYPE_MULTIPART_DATA_FILE_CONTENTTYPE)));
	}

	@Test
	public void shouldInjectParamValueModificationInGeneralParam() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		HttpMessage message = createMessage();
		String paramName = "person";
		String newValue = "injected";
		variant.setMessage(message);
		// When
		variant.setParameter(message,
				new NameValuePair(NameValuePair.TYPE_MULTIPART_DATA_PARAM, paramName, DEFAULT_PARAM_CONTENT, 1),
				paramName, newValue);
		HttpMessage newMsg = createMessage(newValue, DEFAULT_FILE_NAME, DEFAULT_CONTENT_TYPE,
				DEFAULT_FILE_PARAM_CONTENT);
		// Then
		assertThat(message.getRequestBody().toString(), equalTo(newMsg.getRequestBody().toString()));
	}
	
	@Test
	public void shouldInjectParamValueModificationInFileParam() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		HttpMessage message = createMessage();
		String paramName = "somefile";
		String newValue = "injected";
		String origValue = "contents of the file";
		variant.setMessage(message);
		// When
		variant.setParameter(message,
				new NameValuePair(NameValuePair.TYPE_MULTIPART_DATA_FILE_PARAM, paramName, origValue, 2),
				paramName, newValue);
		HttpMessage newMsg = createMessage(DEFAULT_PARAM_CONTENT, DEFAULT_FILE_NAME, DEFAULT_CONTENT_TYPE,
				newValue);
		// Then
		assertThat(message.getRequestBody().toString(), equalTo(newMsg.getRequestBody().toString()));
	}
	
	@Test
	public void shouldInjectParamValueModificationInFileNameParam() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		HttpMessage message = createMessage();
		String paramName = "somefile";
		String newValue = "injected";
		variant.setMessage(message);
		// When
		variant.setParameter(message,
				new NameValuePair(NameValuePair.TYPE_MULTIPART_DATA_FILE_NAME, paramName, DEFAULT_FILE_NAME, 3),
				paramName, newValue);
		HttpMessage newMsg = createMessage(DEFAULT_PARAM_CONTENT, newValue, DEFAULT_CONTENT_TYPE,
				DEFAULT_FILE_PARAM_CONTENT);
		// Then
		assertThat(message.getRequestBody().toString(), equalTo(newMsg.getRequestBody().toString()));
	}
	
	@Test
	public void shouldInjectParamValueModificationInFileContentTypeParam() {
		// Given
		VariantMultipartFormParameters variant = new VariantMultipartFormParameters();
		HttpMessage message = createMessage();
		String paramName = "somefile";
		String newValue = "injected";
		variant.setMessage(message);
		// When
		variant.setParameter(message,
				new NameValuePair(NameValuePair.TYPE_MULTIPART_DATA_FILE_CONTENTTYPE, paramName, DEFAULT_CONTENT_TYPE, 4),
				paramName, newValue);
		HttpMessage newMsg = createMessage(DEFAULT_PARAM_CONTENT, DEFAULT_FILE_NAME, newValue,
				DEFAULT_FILE_PARAM_CONTENT);
		// Then
		assertThat(message.getRequestBody().toString(), equalTo(newMsg.getRequestBody().toString()));
	}
	
	private static HttpMessage createBaseMessage() {
		HttpMessage message = new HttpMessage();
		try {
			message.setRequestHeader("GET / HTTP/1.1\r\n");
			message.getRequestHeader().setHeader(HttpHeader.CONTENT_TYPE, "multipart/form-data; boundary=------------------------d74496d66958873e");
		} catch (HttpMalformedHeaderException e) {
			throw new RuntimeException(e);
		}
		return message;
	}
	
	private static HttpMessage createMessage() {
		return createMessage(DEFAULT_PARAM_CONTENT, DEFAULT_FILE_NAME, DEFAULT_CONTENT_TYPE,
				DEFAULT_FILE_PARAM_CONTENT);
	}

	private static HttpMessage createMessage(String paramContent, String fileName, String contentType, String fileParamContent) {
		/*
		 * --------------------------d74496d66958873e
		 * Content-Disposition: form-data; name="person"
		 * 
		 * anonymous
		 * anonymous2
		 * --------------------------d74496d66958873e
		 * Content-Disposition: form-data; name="somefile"; filename="file.txt"
		 * Content-Type: text/plain
		 * 
		 * contents of the file
		 * --------------------------d74496d66958873e--
		 */
		HttpMessage message = createBaseMessage();
		StringBuilder bodySb = new StringBuilder(320);
		bodySb.append("--------------------------d74496d66958873e").append(CRLF);
		bodySb.append("Content-Disposition: form-data; name=\"person\"").append(CRLF);
		bodySb.append(CRLF);
		bodySb.append(paramContent).append(CRLF);
		bodySb.append("--------------------------d74496d66958873e").append(CRLF);
		bodySb.append("Content-Disposition: form-data; name=\"somefile\"; filename=\"").append(fileName).append("\"").append(CRLF);
		bodySb.append("Content-Type: ").append(contentType).append(CRLF);
		bodySb.append(CRLF);
		bodySb.append(fileParamContent).append(CRLF);
		bodySb.append("--------------------------d74496d66958873e--").append(CRLF);
		message.setRequestBody(bodySb.toString());
		return message;
	}
	
}
