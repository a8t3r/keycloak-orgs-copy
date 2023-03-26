package io.phasetwo.service.model;

import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Stream;

public interface OrganizationPositionModel {
  String getId();

  String getName();

  void setName(String name);

  String getDisplayName();

  void setDisplayName(String displayName);

  List<OrganizationRoleModel> getRoles();

  void grantRole(OrganizationRoleModel role);

  void revokeRole(OrganizationRoleModel role);

  void addSubordinate(OrganizationPositionModel subordinate);

  void removeSubordinate(OrganizationPositionModel subordinate);

  Stream<OrganizationPositionModel> getSubordinateStream();

  String getHeadId();

  void setHead(OrganizationPositionModel head);

  default boolean hasSubordinate(String subPositionId) {
    return getSubordinateStream().anyMatch(it -> it.getId().equals(subPositionId));
  }

  Stream<UserModel> getUserStream();

  default boolean hasUser(UserModel user) {
    return getUserStream().anyMatch(it -> it.getId().equals(user.getId()));
  }

  void addUser(UserModel user);

  void removeUser(UserModel user);

}
