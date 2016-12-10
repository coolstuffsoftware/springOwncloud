package software.coolstuff.springframework.owncloud.service.impl;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import software.coolstuff.springframework.owncloud.config.WithOwncloudMockUser;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.service.AbstractOwncloudUserModificationServiceTest;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

public abstract class AbstractOwncloudUserModificationServiceRestTest extends AbstractOwncloudUserModificationServiceTest implements OwncloudServiceRestTest {

  @Autowired
  private OwncloudUserModificationServiceImpl userModificationService;

  @Autowired
  private OwncloudUserQueryService userQueryService;

  @Override
  public final AbstractOwncloudServiceImpl owncloudService() {
    return userModificationService;
  }

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithoutGroups(OwncloudModificationUser newUser) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET).url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
    postData.put("userid", Lists.newArrayList(newUser.getUsername()));
    postData.put("password", Lists.newArrayList(newUser.getPassword()));
    respondSuccess(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        postData);

    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        true,
        null,
        newUser.getUsername());

    // change the Displayname
    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(newUser.getDisplayName()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        putData);

    // change the eMail
    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(newUser.getEmail()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        putData);

    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername() + "/groups")
            .build());

    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);
    respondUser(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        newUser.isEnabled(),
        newUser.getEmail(),
        newUser.getDisplayName());
    respondGroups(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername() + "/groups")
            .build(),
        CollectionUtils.isEmpty(newUser.getGroups()) ? new String[] {} : newUser.getGroups().toArray(new String[] {}));
  };

  @Override
  protected void prepareTestSaveUser_CreateUser_OK_WithGroups(OwncloudModificationUser newUser) throws Exception {
    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
    postData.put("userid", Lists.newArrayList(newUser.getUsername()));
    postData.put("password", Lists.newArrayList(newUser.getPassword()));
    respondSuccess(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        postData);

    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        true,
        null,
        newUser.getUsername());

    // change the Displayname
    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(newUser.getDisplayName()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        putData);

    // change the eMail
    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(newUser.getEmail()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        putData);

    respondGroups(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername() + "/groups")
            .build());

    for (String group : newUser.getGroups()) {
      postData = new LinkedMultiValueMap<>();
      postData.put("groupid", Lists.newArrayList(group));
      respondSuccess(
          RestRequest.builder()
              .method(POST)
              .url("/cloud/users/" + newUser.getUsername() + "/groups")
              .build(),
          postData);
    }

    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);
    respondUser(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        newUser.isEnabled(),
        newUser.getEmail(),
        newUser.getDisplayName());
    respondGroups(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername() + "/groups")
            .build(),
        CollectionUtils.isEmpty(newUser.getGroups()) ? new String[] {} : newUser.getGroups().toArray(new String[] {}));
  }

  @Override
  protected void prepareTestSaveUser_UpdateUser_OK_WithoutGroups(OwncloudModificationUser existingUser, OwncloudModificationUser updateUser) throws Exception {
    respondUser(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + existingUser.getUsername())
            .build(),
        existingUser.isEnabled(),
        existingUser.getEmail(),
        existingUser.getDisplayName());

    // change the Displayname
    MultiValueMap<String, String> putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("display"));
    putData.put("value", Lists.newArrayList(updateUser.getDisplayName()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + existingUser.getUsername())
            .build(),
        putData);

    // change the eMail
    putData = new LinkedMultiValueMap<>();
    putData.put("key", Lists.newArrayList("email"));
    putData.put("value", Lists.newArrayList(updateUser.getEmail()));
    respondSuccess(
        RestRequest.builder()
            .method(PUT)
            .url("/cloud/users/" + existingUser.getUsername())
            .build(),
        putData);

    if (CollectionUtils.isEmpty(existingUser.getGroups())) {
      respondGroups(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + existingUser.getUsername() + "/groups")
              .build(),
          new String[] {});

      for (String group : existingUser.getGroups()) {
        MultiValueMap<String, String> postData = new LinkedMultiValueMap<>();
        postData = new LinkedMultiValueMap<>();
        postData.put("groupid", Lists.newArrayList(group));
        respondSuccess(
            RestRequest.builder()
                .method(DELETE)
                .url("/cloud/users/" + existingUser.getUsername() + "/groups")
                .build(),
            postData);
      }
    } else {
      respondGroups(
          RestRequest.builder()
              .method(GET)
              .url("/cloud/users/" + existingUser.getUsername() + "/groups")
              .build(),
          existingUser.getGroups().toArray(new String[] {}));
    }

    MockRestServiceServer queryServer = createServer((OwncloudUserQueryServiceImpl) userQueryService);
    respondUser(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + updateUser.getUsername())
            .build(),
        updateUser.isEnabled(),
        updateUser.getEmail(),
        updateUser.getDisplayName());
    respondGroups(
        RestRequest.builder()
            .server(queryServer)
            .method(GET)
            .url("/cloud/users/" + updateUser.getUsername() + "/groups")
            .build(),
        CollectionUtils.isEmpty(updateUser.getGroups()) ? new String[] {} : updateUser.getGroups().toArray(new String[] {}));
  }

  @Test(expected = IllegalArgumentException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_IllegalArgument() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 5")
        .email("user5@example.com")
        .build();

    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        101,
        "Invalid Argument");

    userModificationService.saveUser(newUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_IllegalStateExceptionByStatusCode103() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 5")
        .email("user5@example.com")
        .build();

    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        103,
        "Illegal State");

    userModificationService.saveUser(newUser);
  }

  @Test(expected = AccessDeniedException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_AccessDenied() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 5")
        .email("user5@example.com")
        .build();

    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        997,
        "Access Denied");

    userModificationService.saveUser(newUser);
  }

  @Test(expected = IllegalStateException.class)
  @WithOwncloudMockUser(username = "user1", password = "password")
  public void testSaveUser_CreateUser_NOK_UnknownError() throws Exception {
    OwncloudModificationUser newUser = OwncloudModificationUser.builder()
        .username("user5")
        .password("password")
        .enabled(true)
        .displayName("Mrs. User 5")
        .email("user5@example.com")
        .build();

    respondFailure(
        RestRequest.builder()
            .method(GET)
            .url("/cloud/users/" + newUser.getUsername())
            .build(),
        998,
        "The requested user could not be found");

    respondFailure(
        RestRequest.builder()
            .method(POST)
            .url("/cloud/users")
            .build(),
        999);

    userModificationService.saveUser(newUser);
  }
}
