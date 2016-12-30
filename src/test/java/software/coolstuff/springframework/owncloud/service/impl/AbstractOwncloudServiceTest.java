/*
   Copyright (C) 2016 by the original Authors.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/
package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.SpringBootDependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.util.MultiValueMap;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.coolstuff.springframework.owncloud.config.AuthorityAppenderConfiguration;
import software.coolstuff.springframework.owncloud.config.AuthorityMapperConfiguration;
import software.coolstuff.springframework.owncloud.config.CompareResourceAfter;
import software.coolstuff.springframework.owncloud.config.VelocityConfiguration;
import software.coolstuff.springframework.owncloud.service.api.OwncloudGrantedAuthoritiesMapper;
import software.coolstuff.springframework.owncloud.service.impl.resource.file.OwncloudFileResourceTest;
import software.coolstuff.springframework.owncloud.service.impl.resource.file.OwncloudModifyingFileResourceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.NONE,
    classes = {
        OwncloudAutoConfiguration.class,
        VelocityConfiguration.class,
        AuthorityAppenderConfiguration.class,
        AuthorityMapperConfiguration.class
    })
@TestExecutionListeners({
    SpringBootDependencyInjectionTestExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    WithSecurityContextTestExecutionListener.class,
    OwncloudFileResourceTestExecutionListener.class
})
@Slf4j
public abstract class AbstractOwncloudServiceTest {

  private final static String ORIGINAL_RESOURCE = "classpath:/owncloud.xml";
  private final static String VELOCITY_PATH_PREFIX = "/velocity/";

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired(required = false)
  private OwncloudGrantedAuthoritiesMapper owncloudGrantedAuthoritiesMapper;

  @Autowired(required = false)
  private GrantedAuthoritiesMapper grantedAuthoritiesMapper;

  @Autowired
  private OwncloudProperties properties;

  @Autowired
  private VelocityEngine velocityEngine;

  @Rule
  public TestName testName = new TestName();

  @Autowired(required = false)
  private OwncloudResourceService resourceService;

  private MockRestServiceServer server;

  @Before
  public final void setUp() throws Exception {
    if (this instanceof OwncloudServiceRestTest) {
      server = createServer(((OwncloudServiceRestTest) this).owncloudService());
    }

    if (this instanceof OwncloudFileResourceTest) {
      copyClasspathResourceToFile();
      resourceService.afterPropertiesSet();
    }
  }

  protected final MockRestServiceServer createServer(OwncloudRestService owncloudService) {
    return MockRestServiceServer.createServer(owncloudService.getRestTemplate());
  }

  protected void verifyServer() {
    if (server != null) {
      server.verify();
    }
  }

  protected final MockRestServiceServer getServer() {
    return server;
  }

  private void copyClasspathResourceToFile() throws IOException, FileNotFoundException {
    Resource target = resourceLoader.getResource(properties.getLocation());
    if (!(target instanceof UrlResource)) {
      throw new IllegalStateException(String.format(
          "TestClass %s implements %s but the Resource-Location %s is not of Type %s",
          this.getClass().getName(),
          OwncloudFileResourceTest.class.getName(),
          properties.getLocation(),
          UrlResource.class.getName()));
    }

    try (InputStream is = new BufferedInputStream(getSourceResource().getInputStream());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(target.getFile()))) {
      log.debug("Copy Content of Classpath-Resource {} to File {}", ORIGINAL_RESOURCE, properties.getLocation());
      IOUtils.copy(is, os);
    }
  }

  @After
  public void tearDownResource() throws Throwable {
    if (this instanceof OwncloudFileResourceTest) {
      resourceService.destroy();
      Resource target = resourceLoader.getResource(properties.getLocation());

      boolean hasSpecificResourceTest = false;
      for (Method method : this.getClass().getMethods()) {
        // is this Method annotated by @CompareResourceAfter
        CompareResourceAfter compareResourceAfter = AnnotationUtils.findAnnotation(method, CompareResourceAfter.class);
        if (compareResourceAfter == null || !StringUtils.equals(compareResourceAfter.value(), testName.getMethodName())) {
          continue;
        }

        // a Method annotated by @Test cannot also be annotated by
        // @CompareResourceAfter
        if (AnnotationUtils.findAnnotation(method, Test.class) != null) {
          log.warn("Method {} of Class {} cannot be annotated by {} and {}", method.getName(), this.getClass().getName(), CompareResourceAfter.class, Test.class);
          continue;
        }

        // the @CompareResourceAfter annotated Method must have exactly 2
        // Parameters of Type org.springframework.core.io.Resource
        if (method.getParameterCount() != 1) {
          log.warn("Method {} of Class {} is annotated by {} but has {} Parameters instead of 1",
              method.getName(),
              this.getClass().getName(),
              CompareResourceAfter.class.getName(),
              method.getParameterCount());
          continue;
        }
        boolean correctParameterTypes = true;
        for (Class<?> parameterClass : method.getParameterTypes()) {
          correctParameterTypes = correctParameterTypes && Resource.class.isAssignableFrom(parameterClass);
        }
        if (!correctParameterTypes) {
          log.warn("Method {} of Class {} (annotated by {}) must have 1 Parameter of Type {}",
              method.getName(),
              this.getClass(),
              CompareResourceAfter.class.getName(),
              Resource.class.getName());
          continue;
        }

        log.debug("Call the Resource Comparsion Method {} on Class {}", method.getName(), this.getClass().getName());
        hasSpecificResourceTest = true;
        try {
          method.invoke(this, target);
        } catch (InvocationTargetException e) {
          throw e.getCause();
        }
      }

      if (!hasSpecificResourceTest && ((OwncloudFileResourceTest) this).isCheckAllResourcesAgainstOriginal()) {
        compareResourcesWithOriginalSource(target);
      }
    }
  }

  protected void respondUsers(RestRequest request, String... users) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    context.put("users", Arrays.asList(users));

    preparedRequest.andRespond(withSuccess(merge("users.vm", context), MediaType.TEXT_XML));
  }

  private boolean isNoRestTestClass() {
    return !(this instanceof OwncloudServiceRestTest);
  }

  private ResponseActions prepareRestRequest(RestRequest request) throws MalformedURLException {
    OwncloudServiceRestTest restTest = (OwncloudServiceRestTest) this;
    MockRestServiceServer server = this.server;
    if (request.getServer() != null) {
      server = request.getServer();
    }
    ResponseActions responseActions = server
        .expect(requestToWithPrefix(request.getUrl()))
        .andExpect(method(request.getMethod()));
    if (StringUtils.isNotBlank(request.getBasicAuthentication())) {
      responseActions.andExpect(header(HttpHeaders.AUTHORIZATION, request.getBasicAuthentication()));
    } else {
      responseActions.andExpect(header(HttpHeaders.AUTHORIZATION, restTest.getBasicAuthorizationHeader()));
    }
    return responseActions;
  }

  private RequestMatcher requestToWithPrefix(String uri) throws MalformedURLException {
    String rootURI = null;
    if (OwncloudUtils.isNoResourceLocation(properties.getLocation())) {
      URL url = new URL(properties.getLocation());
      rootURI = properties.getLocation();
      if (StringUtils.isBlank(url.getPath()) || "/".equals(url.getPath())) {
        rootURI = URI.create(url.toString() + AbstractOwncloudRestServiceImpl.DEFAULT_PATH).toString();
      }
    }
    return requestTo(rootURI + uri);
  }

  private void setSuccessMetaInformation(Context context) {
    context.put("status", "ok");
    context.put("statuscode", 100);
  }

  private String merge(String templateName, Context context) throws IOException {
    String prefixedTemplateName = templateName;
    if (!StringUtils.startsWith(templateName, VELOCITY_PATH_PREFIX)) {
      prefixedTemplateName = VELOCITY_PATH_PREFIX + templateName;
      if (StringUtils.startsWith(templateName, "/")) {
        prefixedTemplateName = VELOCITY_PATH_PREFIX + StringUtils.substring(templateName, 1);
      }
    }
    Template template = velocityEngine.getTemplate(prefixedTemplateName);
    try (Writer writer = new StringWriter()) {
      template.merge(context, writer);
      writer.flush();
      return writer.toString();
    }
  }

  protected void respondGroups(RestRequest request, String... groups) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    context.put("groups", Arrays.asList(groups));

    preparedRequest.andRespond(withSuccess(merge("groups.vm", context), request.getResponseType()));
  }

  protected void respondFailure(RestRequest request, int statuscode) throws IOException {
    respondFailure(request, statuscode, null);
  }

  protected void respondFailure(RestRequest request, int statuscode, String message) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setFailureMetaInformation(context, statuscode, message);

    preparedRequest.andRespond(withSuccess(merge("void.vm", context), request.getResponseType()));
  }

  protected void respondHttpStatus(RestRequest request, HttpStatus httpStatus) throws MalformedURLException {
    if (isNoRestTestClass()) {
      return;
    }
    prepareRestRequest(request).andRespond(withStatus(httpStatus));
  }

  protected void respondSuccess(RestRequest request) throws IOException {
    respondSuccess(request, null);
  }

  protected void respondSuccess(RestRequest request, MultiValueMap<String, String> requestBody) throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);

    if (requestBody != null) {
      preparedRequest = preparedRequest.andExpect(content().formData(requestBody));
    }
    preparedRequest.andRespond(withSuccess(merge("void.vm", context), MediaType.TEXT_XML));
  }

  private void setFailureMetaInformation(Context context, int statuscode, String message) {
    context.put("status", "failure");
    context.put("statuscode", statuscode);
    context.put("message", "message");
  }

  protected void respondUser(RestRequest request, boolean enabled, String email, String displayName)
      throws IOException {
    if (isNoRestTestClass()) {
      return;
    }
    ResponseActions preparedRequest = prepareRestRequest(request);

    Context context = new VelocityContext();
    setSuccessMetaInformation(context);
    context.put("enabled", Boolean.toString(enabled));
    context.put("email", email);
    context.put("displayName", displayName);

    preparedRequest.andRespond(withSuccess(merge("user.vm", context), MediaType.TEXT_XML));
  }

  protected Resource getResourceOf(String testCase) {
    if (!(this instanceof OwncloudModifyingFileResourceTest)) {
      return null;
    }

    OwncloudModifyingFileResourceTest modifyingFileResourceTest = (OwncloudModifyingFileResourceTest) this;

    String path = "/";
    if (StringUtils.isNotBlank(modifyingFileResourceTest.getResourcePrefix())) {
      if (StringUtils.startsWith(modifyingFileResourceTest.getResourcePrefix(), "/")) {
        path = StringUtils.appendIfMissing(modifyingFileResourceTest.getResourcePrefix(), "/");
      } else {
        path += StringUtils.appendIfMissing(modifyingFileResourceTest.getResourcePrefix(), "/");
      }
    }

    return resourceLoader.getResource("classpath:" + path + testCase + ".xml");
  }

  protected void checkAuthorities(String username, Collection<? extends GrantedAuthority> actual, String... expected) {
    Assert.assertEquals(expected.length, actual == null ? 0 : actual.size());
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (ArrayUtils.isNotEmpty(expected)) {
      for (String authority : expected) {
        authorities.add(new SimpleGrantedAuthority(authority));
      }
    }
    if (owncloudGrantedAuthoritiesMapper != null) {
      Assert.assertTrue(CollectionUtils.isEqualCollection(actual, owncloudGrantedAuthoritiesMapper.mapAuthorities(username, authorities)));
    } else if (grantedAuthoritiesMapper != null) {
      Assert.assertTrue(CollectionUtils.isEqualCollection(actual, grantedAuthoritiesMapper.mapAuthorities(authorities)));
    } else {
      Assert.assertTrue(CollectionUtils.isEqualCollection(actual, authorities));
    }
  }

  protected Resource getSourceResource() {
    Resource resource = resourceLoader.getResource(ORIGINAL_RESOURCE);
    if (resource == null) {
      throw new IllegalStateException("Source Resource " + ORIGINAL_RESOURCE + " is not available");
    }
    return resource;
  }

  protected void compareResourcesWithOriginalSource(Resource target) throws Exception {
    compareResources(getSourceResource(), target);
  }

  protected void compareResources(Resource source, Resource target) throws Exception {
    try (InputStream inputSource = new BufferedInputStream(source.getInputStream());
        InputStream inputTarget = new BufferedInputStream(target.getInputStream())) {
      Diff diff = DiffBuilder.compare(Input.fromStream(inputSource))
          .withTest(Input.fromStream(inputTarget))
          .checkForSimilar()
          .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
          .build();
      Assert.assertFalse(diff.toString(), diff.hasDifferences());
    }
  }

  protected final String getDefaultBasicAuthorizationHeader() {
    return "Basic " + Base64.getEncoder().encodeToString((properties.getUsername() + ":" + properties.getPassword()).getBytes());
  }

  protected final String getSecurityContextBasicAuthorizationHeader() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return "Basic " + Base64.getEncoder().encodeToString((authentication.getName() + ":" + authentication.getCredentials()).getBytes());
  }

  @Data
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  protected static class RestRequest {

    private MockRestServiceServer server;
    @NotNull
    private final HttpMethod method;
    @NotNull
    private final String url;
    private String basicAuthentication;
    private MediaType responseType = MediaType.TEXT_XML;

    public static class RestRequestBuilder {
      private MediaType responseType = MediaType.TEXT_XML;
    }
  }
}
