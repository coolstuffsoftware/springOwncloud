/*-
 * #%L
 * owncloud-spring-boot-starter
 * %%
 * Copyright (C) 2016 - 2017 by the original Authors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package software.coolstuff.springframework.owncloud.service.api;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import software.coolstuff.springframework.owncloud.exception.auth.OwncloudGroupNotFoundException;
import software.coolstuff.springframework.owncloud.exception.auth.OwncloudUsernameAlreadyExistsException;
import software.coolstuff.springframework.owncloud.model.OwncloudModificationUser;
import software.coolstuff.springframework.owncloud.model.OwncloudUserDetails;

/**
 * Get and modify Information of Users on the Owncloud Server.
 * <p/>
 * This Service is only usable when the User has been authenticated
 * by the OwncloudAuthenticationProvider.
 *
 * @author mufasa1976
 * @since 1.0.0
 */
public interface OwncloudUserService {

  /**
   * Get the Details of a User or <code>Optional.empty()</code> if
   * the User doesn&apos;t exist
   * @param username Name of the User
   * @return Details of the User
   * @throws AccessDeniedException Neither an Administrator nor the Owner
   */
  Optional<OwncloudUserDetails> findOne(String username);

  /**
   * Find all Users.
   * @return List of all Users
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAll();

  /**
   * Find all Users whose Display Name confirm to the Filter Criterion.
   * @param filter Filter Criterion for Display Name
   * @return List of all Users whose Display Name confirm to the Filter Criterion
   * @throws AccessDeniedException Not an Administrator
   */
  List<String> findAll(String filter);

  /**
   * Create or update a User.
   *
   * @param user
   *          User Details to be saved
   * @return saved User Details
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties.isEnableModifications()</code> or neither an Administrator nor the Owner
   * @throws OwncloudUsernameAlreadyExistsException
   *           the User already exists (minimal chance to get this Exception)
   * @throws OwncloudGroupNotFoundException
   *           referenced Group doesn&apos;t exist (either while adding or removing Membership)
   */
  OwncloudUserDetails save(OwncloudModificationUser user);

  /**
   * Remove a User.
   *
   * @param username
   *          Name of the User to be removed
   * @throws AccessDeniedException
   *           either Modifications are not allowed <code>OwncloudProperties#isEnableModifications()</code> or neither an Administrator nor the Owner
   * @throws UsernameNotFoundException
   *           the User doesn&apos;t exist anymore
   */
  void delete(String username);

}
