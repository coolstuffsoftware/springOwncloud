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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;

import software.coolstuff.springframework.owncloud.exception.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;
import software.coolstuff.springframework.owncloud.service.api.OwncloudUserQueryService;

class OwncloudUserQueryRestServiceImpl extends AbstractOwncloudRestServiceImpl implements OwncloudUserQueryService {

  @Autowired
  private OwncloudUserDetailsConversionService conversionService;

  OwncloudUserQueryRestServiceImpl(RestTemplateBuilder builder) {
    super(builder);
  }

  @Override
  public List<String> findAllUsers() {
    return findAllUsers(null);
  }

  @Override
  public List<String> findAllUsers(String filter) {
    Ocs.Users users = null;
    if (StringUtils.isBlank(filter)) {
      users = exchange("/cloud/users", HttpMethod.GET, emptyEntity(), Ocs.Users.class);
    } else {
      users = exchange("/cloud/users?search={filter}", HttpMethod.GET, emptyEntity(), Ocs.Users.class, filter);
    }
    return convertUsers(users);
  }

  private List<String> convertUsers(Ocs.Users ocsUsers) {
    List<String> users = new ArrayList<>();
    if (isUsersNotNull(ocsUsers)) {
      for (Ocs.Users.Data.Element element : ocsUsers.getData().getUsers()) {
        users.add(element.getElement());
      }
    }
    return users;
  }

  private boolean isUsersNotNull(Ocs.Users ocsUsers) {
    return ocsUsers != null && ocsUsers.getData() != null && ocsUsers.getData().getUsers() != null;
  }

  @Override
  public List<String> findAllGroups() {
    return findAllGroups(null);
  }

  @Override
  public List<String> findAllGroups(String filter) {
    Ocs.Groups ocsGroups = null;
    if (StringUtils.isBlank(filter)) {
      ocsGroups = exchange("/cloud/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class);
    } else {
      ocsGroups = exchange("/cloud/groups?search={filter}", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, filter);
    }
    return convertGroups(ocsGroups);
  }

  public static List<String> convertGroups(Ocs.Groups ocsGroups) {
    List<String> groups = new ArrayList<>();
    if (isGroupsNotNull(ocsGroups)) {
      for (Ocs.Groups.Data.Group group : ocsGroups.getData().getGroups()) {
        groups.add(group.getGroup());
      }
    }
    return groups;
  }

  private static boolean isGroupsNotNull(Ocs.Groups ocsGroups) {
    return ocsGroups != null && ocsGroups.getData() != null && ocsGroups.getData().getGroups() != null;
  }

  @Override
  public List<String> findAllMembersOfGroup(String groupname) {
    Validate.notBlank(groupname);
    Ocs.Users users = exchange("/cloud/groups/{group}", HttpMethod.GET, emptyEntity(), Ocs.Users.class, (uri, meta) -> {
      if ("ok".equals(meta.getStatus())) {
        return;
      }

      switch (meta.getStatuscode()) {
        case 997:
          throw new AccessDeniedException("Not Authorized to access Resource " + uri);
        case 998:
          throw new OwncloudGroupNotFoundException(groupname);
        default:
          throw new IllegalStateException("Unknown Error Code " + meta.getStatuscode() + ". Reason: " + meta.getMessage());
      }
    }, groupname);
    return convertUsers(users);
  }

  @Override
  public List<String> findAllGroupsOfUser(String username) {
    Validate.notBlank(username);
    Ocs.Groups ocsGroups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return convertGroups(ocsGroups);
  }

  @Override
  public OwncloudUserDetails findOneUser(String username) {
    Validate.notBlank(username);
    Ocs.User user = exchange("/cloud/users/{user}", HttpMethod.GET, emptyEntity(), Ocs.User.class, username);
    Ocs.Groups groups = exchange("/cloud/users/{user}/groups", HttpMethod.GET, emptyEntity(), Ocs.Groups.class, username);
    return conversionService.convert(username, user, groups);
  }

}