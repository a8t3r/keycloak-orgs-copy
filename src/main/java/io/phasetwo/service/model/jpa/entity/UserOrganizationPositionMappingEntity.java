package io.phasetwo.service.model.jpa.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Table(
    name = "USER_ORGANIZATION_POSITION_MAPPING",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"POSITION_ID", "USER_ID"})}
)
public class UserOrganizationPositionMappingEntity {
  @Id
  @Column(name = "ID", length = 36)
  @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This
  // avoids an extra SQL
  protected String id;

  @NotNull
  @Column(name = "USER_ID", nullable = false)
  protected String userId;

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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
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
