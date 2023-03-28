package io.phasetwo.service.model.jpa.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@Entity
@Table(
    name = "ORGANIZATION_POSITION",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"ORGANIZATION_ID", "NAME"})}
)
public class OrganizationPositionEntity {

  public static String TOP_PARENT_ID = " ";

  @Id
  @Column(name = "ID", length = 36)
  @Access(AccessType.PROPERTY)
  protected String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORGANIZATION_ID")
  protected OrganizationEntity organization;

  @Column(name = "NAME", nullable = false)
  protected String name;

  @Column(name = "DISPLAY_NAME")
  protected String displayName;

  @Column(name = "PARENT_ID")
  protected String parentId;

  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = "position")
  protected Collection<UserOrganizationPositionMappingEntity> userMappings = new ArrayList<>();

  @OneToMany(
          fetch = FetchType.LAZY,
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          mappedBy = "position")
  protected Collection<OrganizationPositionRolesMappingEntity> roleMappings = new ArrayList<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OrganizationEntity getOrganization() {
    return organization;
  }

  public void setOrganization(OrganizationEntity organization) {
    this.organization = organization;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Collection<UserOrganizationPositionMappingEntity> getUserMappings() {
    return userMappings;
  }

  public void setUserMappings(Collection<UserOrganizationPositionMappingEntity> userMappings) {
    this.userMappings = userMappings;
  }

  public Collection<OrganizationPositionRolesMappingEntity> getRoleMappings() {
    return roleMappings;
  }

  public void setRoleMappings(Collection<OrganizationPositionRolesMappingEntity> roleMappings) {
    this.roleMappings = roleMappings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrganizationPositionEntity that = (OrganizationPositionEntity) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
