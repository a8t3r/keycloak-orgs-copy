package io.phasetwo.service.model.jpa.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
    name = "ORGANIZATION_POSITION_ROLES",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"POSITION_ID", "ROLE_ID"})}
)
public class OrganizationPositionRolesMappingEntity {
  @Id
  @Column(name = "ID", length = 36)
  @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This
  // avoids an extra SQL
  protected String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ROLE_ID")
  protected OrganizationRoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "POSITION_ID")
  protected OrganizationPositionEntity position;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "CREATED_AT")
  protected Date createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) createdAt = new Date();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OrganizationRoleEntity getRole() {
    return role;
  }

  public void setRole(OrganizationRoleEntity role) {
    this.role = role;
  }

  public OrganizationPositionEntity getPosition() {
    return position;
  }

  public void setPosition(OrganizationPositionEntity position) {
    this.position = position;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }
}
