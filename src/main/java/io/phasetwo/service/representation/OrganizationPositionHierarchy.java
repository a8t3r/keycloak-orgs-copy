package io.phasetwo.service.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrganizationPositionHierarchy {

  private @Valid String id = null;
  private @Valid String name = null;
  private @Valid String displayName = null;
  private @Valid List<String> roleNames = new ArrayList<>();
  private @Valid List<String> userIds = new ArrayList<>();
  private @Valid List<OrganizationPositionHierarchy> subordinates = new ArrayList<>();

  public OrganizationPositionHierarchy id(String id) {
    this.id = id;
    return this;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OrganizationPositionHierarchy name(String name) {
    this.name = name;
    return this;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OrganizationPositionHierarchy displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @JsonProperty("roleNames")
  public List<String> getRoleNames() {
    return roleNames;
  }

  public void setRoleNames(List<String> roleNames) {
    this.roleNames = roleNames;
  }

  public OrganizationPositionHierarchy roleNames(List<String> roleNames) {
    this.roleNames = roleNames;
    return this;
  }

  @JsonProperty("userIds")
  public List<String> getUserIds() {
    return userIds;
  }

  public void setUserIds(List<String> userIds) {
    this.userIds = userIds;
  }

  public OrganizationPositionHierarchy userIds(List<String> userIds) {
    this.userIds = userIds;
    return this;
  }

  @JsonProperty("subordinates")
  public List<OrganizationPositionHierarchy> getSubordinates() {
    return subordinates;
  }

  public void setSubordinates(List<OrganizationPositionHierarchy> subordinates) {
    this.subordinates = subordinates;
  }

  public OrganizationPositionHierarchy subordinates(List<OrganizationPositionHierarchy> subordinates) {
    this.subordinates = subordinates;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OrganizationPositionHierarchy organizationRole = (OrganizationPositionHierarchy) o;
    return Objects.equals(id, organizationRole.id) && Objects.equals(name, organizationRole.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OrganizationPosition {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    roleNames: ").append(toIndentedString(roleNames)).append("\n");
    sb.append("    subordinates: ").append(toIndentedString(subordinates)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
