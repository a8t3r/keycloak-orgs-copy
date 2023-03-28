package io.phasetwo.service.resource;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationPositionModel;
import io.phasetwo.service.model.OrganizationRoleModel;
import io.phasetwo.service.representation.OrganizationPosition;
import io.phasetwo.service.representation.OrganizationPositionHierarchy;
import io.phasetwo.service.representation.OrganizationRole;
import io.phasetwo.service.util.PositionUtils;
import lombok.extern.jbosslog.JBossLog;
import org.apache.commons.lang3.BooleanUtils;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.phasetwo.service.resource.Converters.*;
import static io.phasetwo.service.resource.OrganizationResourceType.*;
import static io.phasetwo.service.util.PositionUtils.expandPositionsStream;
import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;

@JBossLog
public class PositionResource extends OrganizationAdminResource {

  private final OrganizationModel organization;
  private final OrganizationPositionModel position;

  protected PositionResource(OrganizationAdminResource parent, OrganizationModel organization, OrganizationPositionModel position) {
    super(parent);
    this.organization = organization;
    this.position = position;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public OrganizationPosition get() {
    return Converters.convertPositionModelToPosition(position);
  }

  @PUT
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(OrganizationPosition rep) {
    canManage();

    organization.getPositionByName(rep.getName())
        .filter(it -> !Objects.equals(position.getId(), it.getId()))
        .ifPresent(it -> {
          throw new ClientErrorException(Response.Status.CONFLICT);
        });

    if (!Objects.equals(position.getDisplayName(), rep.getDisplayName()) || !Objects.equals(position.getName(), rep.getName())) {
      position.setName(rep.getName());
      position.setDisplayName(rep.getDisplayName());

      OrganizationPosition op = convertPositionModelToPosition(position);
      adminEvent
          .resource(ORGANIZATION_POSITION.name())
          .operation(OperationType.UPDATE)
          .resourcePath(session.getContext().getUri(), op.getId())
          .representation(op)
          .success();
    }

    return Response.noContent().build();
  }

  @DELETE
  @Path("")
  public Response delete() {
    canManage();

    organization.removePosition(position);

    adminEvent
        .resource(ORGANIZATION_POSITION.name())
        .operation(OperationType.DELETE)
        .resourcePath(session.getContext().getUri(), position.getId())
        .success();

    return Response.noContent().build();
  }

  @GET
  @Path("/hierarchy")
  public OrganizationPositionHierarchy getHierarchy() {
    return convertPositionModelToPositionHierarchy(position, new HashSet<>());
  }

  private OrganizationPositionHierarchy convertPositionModelToPositionHierarchy(OrganizationPositionModel model, Set<String> visited) {
    return new OrganizationPositionHierarchy()
        .id(model.getId())
        .name(model.getName())
        .displayName(model.getDisplayName())
        .userIds(model.getUserStream().map(UserModel::getId).collect(Collectors.toList()))
        .roleNames(model.getRoles().stream().map(OrganizationRoleModel::getName).collect(Collectors.toList()))
        .subordinates(model.getSubordinateStream()
            .filter(it -> visited.add(it.getId()))
            .map(it -> convertPositionModelToPositionHierarchy(it, visited))
            .collect(Collectors.toList()));
  }

  @GET
  @Path("/subordinates")
  public Stream<OrganizationPosition> getSubordinates(@QueryParam("includeInherited") Boolean includeInherited) {
    Stream<OrganizationPositionModel> subordinateStream = BooleanUtils.isNotTrue(includeInherited) ?
        position.getSubordinateStream() :
        expandPositionsStream(position.getSubordinateStream());

    return subordinateStream.map(Converters::convertPositionModelToPosition);
  }

  @POST
  @Path("/subordinates")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createSubordinate(OrganizationPosition rep) {
    canManage();

    OrganizationPositionModel subordinate;
    Optional<OrganizationPositionModel> subordinateOpt = rep.getId() != null ?
        organization.getPositionById(rep.getId()) : organization.getPositionByName(rep.getName());

    if (subordinateOpt.isPresent()) {
      // remove from previous owner
      subordinate = subordinateOpt.get();
      if (subordinate.getHeadId() != null) {
        OrganizationPositionModel head = organization.getPositionById(subordinate.getHeadId())
            .orElseThrow(NotFoundException::new);
        removeSubordinate(subordinate.getId(), head);
      }

      if (Objects.equals(subordinate.getId(), position.getId())) {
        throw new BadRequestException(
            String.format("Position %s couldn't be subordinated by itself", rep.getName()));
      }

      position.addSubordinate(subordinate);
    } else {
      subordinate = organization.addPosition(rep.getName(), position);
      subordinate.setDisplayName(rep.getDisplayName());
    }

    OrganizationPosition position = convertPositionModelToPosition(subordinate);
    adminEvent
        .resource(ORGANIZATION_POSITION_SUBORDINATE.name())
        .operation(OperationType.CREATE)
        .resourcePath(session.getContext().getUri(), position.getName())
        .representation(position)
        .success();

    return Response.created(
            session.getContext().getUri().getAbsolutePathBuilder().path(position.getId()).build())
        .build();
  }

  @GET
  @Path("/subordinates/{positionId}")
  public Response isSubordinated(@PathParam("positionId") String positionId) {
    if (position.hasSubordinate(positionId)) {
      return Response.noContent().build();
    } else {
      throw new NotFoundException();
    }
  }

  @DELETE
  @Path("/subordinates/{positionId}")
  public Response removeSubordinate(@PathParam("positionId") String positionId) {
    removeSubordinate(positionId, position);
    return Response.noContent().build();
  }

  public void removeSubordinate(String positionId, OrganizationPositionModel position) {
    Optional<OrganizationPositionModel> subordinateOpt = organization.getPositionById(positionId);
    if (subordinateOpt.isPresent() && position.hasSubordinate(positionId)) {
      position.removeSubordinate(subordinateOpt.get());
      adminEvent
          .resource(ORGANIZATION_POSITION_SUBORDINATE.name())
          .operation(OperationType.DELETE)
          .resourcePath(session.getContext().getUri())
          .representation(positionId)
          .success();
    } else {
      throw new NotFoundException(String.format("Position %s doesn't have subordinate with id %s", position.getName(), positionId));
    }
  }

  @GET
  @Path("/users")
  @Produces(MediaType.APPLICATION_JSON)
  public Stream<UserRepresentation> getPositionUsers() {
    return position.getUserStream().map(it -> toRepresentation(session, realm, it));
  }

  @PUT
  @Path("/users/{userId}")
  public Response assignUserToPosition(@PathParam("userId") String userId) {
    canManage();

    UserModel user = session.users().getUserById(realm, userId);
    if (user != null) {
      if (!organization.hasMembership(user)) {
        throw new BadRequestException(
            String.format(
                "User %s must be a member of %s to be granted role.",
                userId, organization.getName()));
      }
      if (!position.hasUser(user)) {
        position.addUser(user);

        adminEvent
            .resource(ORGANIZATION_POSITION_USER_MEMBERSHIP.name())
            .operation(OperationType.CREATE)
            .resourcePath(session.getContext().getUri())
            .representation(userId)
            .success();
      }

      return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).build();
    } else {
      throw new NotFoundException(String.format("User %s doesn't exist", userId));
    }
  }

  @GET
  @Path("/users/{userId}")
  public Response isAssignedToPosition(@PathParam("userId") String userId) {
    UserModel user = session.users().getUserById(realm, userId);
    if (user != null && position.hasUser(user)) {
      return Response.noContent().build();
    } else {
      throw new NotFoundException(String.format("User %s doesn't assigned to position %s", userId, position.getName()));
    }
  }

  @DELETE
  @Path("/users/{userId}")
  public Response removeUserFromPosition(@PathParam("userId") String userId) {
    canManage();

    UserModel user = session.users().getUserById(realm, userId);
    if (user != null && position.hasUser(user)) {
      position.removeUser(user);
      adminEvent
          .resource(ORGANIZATION_POSITION_USER_MEMBERSHIP.name())
          .operation(OperationType.DELETE)
          .resourcePath(session.getContext().getUri())
          .representation(userId)
          .success();
      return Response.noContent().build();
    } else {
      throw new NotFoundException(String.format("User %s doesn't assigned to position %s", userId, position.getName()));
    }
  }

  @GET
  @Path("/roles")
  @Produces(MediaType.APPLICATION_JSON)
  public Stream<OrganizationRole> getPositionRoles(@QueryParam("includeInherited") Boolean includeInherited) {
    Stream<OrganizationRoleModel> roleStream = BooleanUtils.isNotTrue(includeInherited) ?
        position.getRoles().stream() :
        PositionUtils.expandPosition(position)
            .flatMap(it -> it.getRoles().stream())
            .distinct();

    return roleStream.map(Converters::convertOrganizationRole);
  }

  @PUT
  @Path("/roles/{roleName}")
  public Response grantRole(@PathParam("roleName") String roleName) {
    canManage();

    OrganizationRoleModel role = organization.getRoleByName(roleName);
    if (role == null) {
      throw new NotFoundException();
    }

    position.grantRole(role);
    adminEvent
        .resource(ORGANIZATION_POSITION_ROLE_MAPPING.name())
        .operation(OperationType.CREATE)
        .resourcePath(session.getContext().getUri())
        .representation(roleName)
        .success();

    return Response.created(session.getContext().getUri().getAbsolutePathBuilder().build()).build();
  }

  @DELETE
  @Path("/roles/{roleName}")
  public Response revokeRole(@PathParam("roleName") String roleName) {
    canManage();

    OrganizationRoleModel role = position.getRoles().stream()
        .filter(it -> it.getName().equals(roleName))
        .findFirst()
        .orElseThrow(NotFoundException::new);

    position.revokeRole(role);

    adminEvent
        .resource(ORGANIZATION_POSITION_ROLE_MAPPING.name())
        .operation(OperationType.DELETE)
        .resourcePath(session.getContext().getUri())
        .representation(roleName)
        .success();

    return Response.noContent().build();
  }

  private void canManage() {
    if (!auth.hasManageOrgs() && !auth.hasOrgManagePositions(organization)) {
      throw new NotAuthorizedException(
          String.format(
              "User %s doesn't have permission to manage positions in org %s",
              auth.getUser().getId(), organization.getName()));
    }
  }
}
