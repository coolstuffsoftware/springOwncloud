/*
   Copyright (C) 2017 by the original Authors.

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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import lombok.Builder;
import lombok.Data;
import software.coolstuff.springframework.owncloud.model.OwncloudQuota;

/**
 * REST Implementation of the {@link OwncloudQuota}
 */
@Data
@Builder
public class OwncloudRestQuotaImpl implements OwncloudQuota, Cloneable {
  /** Username */
  private final String username;

  /** Free Space */
  private long free;

  /** used Space */
  private long used;

  /** total Quota of the User */
  private long total;

  /** Percent of used Space */
  private float relative;
}