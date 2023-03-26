package io.phasetwo.service.model.jpa;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationPositionModel;
import io.phasetwo.service.model.OrganizationRoleModel;
import io.phasetwo.service.model.jpa.entity.OrganizationPositionEntity;
import io.phasetwo.service.model.jpa.entity.OrganizationPositionRolesMappingEntity;
import io.phasetwo.service.model.jpa.entity.UserOrganizationPositionMappingEntity;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.JpaModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.phasetwo.service.model.jpa.entity.OrganizationPositionEntity.TOP_PARENT_ID;
import static org.keycloak.utils.StreamsUtil.closing;

public class OrganizationPositionAdapter
        implements OrganizationPositionModel, JpaModel<OrganizationPositionEntity> {

  protected final KeycloakSession session;
  protected final OrganizationPositionEntity position;
  protected final EntityManager em;
  protected final OrganizationModel organization;

  public OrganizationPositionAdapter(KeycloakSession session, OrganizationPositionEntity position, EntityManager em, OrganizationModel organization) {
    this.session = session;
    this.position = position;
    this.em = em;
    this.organization = organization;
  }

  @Override
  public OrganizationPositionEntity getEntity() {
    return position;
  }

  @Override
  public String getId() {
    return position.getId();
  }

  @Override
  public String getName() {
    return position.getName();
  }

  @Override
  public void setName(String name) {
    position.setName(name);
  }

  @Override
  public String getDisplayName() {
    return position.getDisplayName();
  }

  @Override
  public void setDisplayName(String displayName) {
    position.setDisplayName(displayName);
  }

  @Override
  public List<OrganizationRoleModel> getRoles() {
    return position.getRoleMappings().stream()
            .map(OrganizationPositionRolesMappingEntity::getRole)
            .map(it -> new OrganizationRoleAdapter(session, organization.getRealm(), em, it))
            .collect(Collectors.toList());
  }

  @Override
  public void grantRole(OrganizationRoleModel role) {
    revokeRole(role);
    OrganizationPositionRolesMappingEntity m = new OrganizationPositionRolesMappingEntity();
    m.setId(KeycloakModelUtils.generateId());
    m.setPosition(position);
    m.setRole(role.getEntity());
    em.persist(m);
    position.getRoleMappings().add(m);
  }

  @Override
  public void revokeRole(OrganizationRoleModel role) {
    position.getRoleMappings().removeIf(m -> m.getRole().getId().equals(role.getId()));
  }

  @Override
  public void addSubordinate(OrganizationPositionModel subordinate) {
    subordinate.setHead(this);
  }

  @Override
  public void removeSubordinate(OrganizationPositionModel subordinate) {
    subordinate.setHead(null);
  }

  @Override
  public Stream<OrganizationPositionModel> getSubordinateStream() {
    return organization.getPositionsStream()
        .filter(it -> Objects.equals(it.getHeadId(), position.getId()));
  }

  @Override
  public String getHeadId() {
    String parentId = position.getParentId();
    return Objects.equals(parentId, TOP_PARENT_ID) ? null : parentId;
  }

  @Override
  public void setHead(OrganizationPositionModel head) {
    String parentId = head == null ? TOP_PARENT_ID : head.getId();
    position.setParentId(parentId);
  }

  @Override
  public Stream<UserModel> getUserStream() {
    return position.getUserMappings().stream()
            .map(UserOrganizationPositionMappingEntity::getUserId)
            .map(it -> session.users().getUserById(organization.getRealm(), it));
  }

  @Override
  public void addUser(UserModel user) {
    removeUser(user);
    UserOrganizationPositionMappingEntity m = new UserOrganizationPositionMappingEntity();
    m.setId(KeycloakModelUtils.generateId());
    m.setUserId(user.getId());
    m.setPosition(position);
    em.persist(m);
    position.getUserMappings().add(m);
  }

  @Override
  public void removeUser(UserModel user) {
    position.getUserMappings().removeIf(it -> it.getUserId().equals(user.getId()));
  }
}
