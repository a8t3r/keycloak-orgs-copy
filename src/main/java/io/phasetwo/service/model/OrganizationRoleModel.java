package io.phasetwo.service.model;

import java.util.stream.Stream;

import io.phasetwo.service.model.jpa.entity.OrganizationRoleEntity;
import org.keycloak.models.UserModel;

public interface OrganizationRoleModel {

  OrganizationRoleEntity getEntity();

  String getId();

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  Stream<UserModel> getUserMappingsStream();

  void grantRole(UserModel user);

  void revokeRole(UserModel user);

  boolean hasRole(UserModel user);
}
