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
package software.coolstuff.springframework.owncloud.service.impl.rest;

import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import software.coolstuff.springframework.owncloud.service.impl.OwncloudProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ConfigurationProperties("owncloud")
class OwncloudRestProperties extends OwncloudProperties {

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  static class ResourceServiceProperties extends OwncloudProperties.ResourceServiceProperties {

    @Data
    public static class CacheProperties {
      @NotNull
      private Class<? extends SardineCacheLoader> cacheLoaderClass = SardineCacheLoader.class;
      private Integer concurrencyLevel;
      private Long expireAfterAccess;
      private TimeUnit expireAfterAccessTimeUnit = TimeUnit.SECONDS;
      private Long expireAfterWrite;
      private TimeUnit expireAfterWriteTimeUnit = TimeUnit.SECONDS;
      private Integer initialCapacity;
      private Long maximumSize;
      private Long maximumWeight;
      private Long refreshAfterWrite;
      private TimeUnit refreshAfterWriteTimeUnit;
    }

    private CacheProperties cache = new CacheProperties();

  }

  private ResourceServiceProperties resourceService = new ResourceServiceProperties();

}
