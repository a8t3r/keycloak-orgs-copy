package io.phasetwo.service.resource;

import io.phasetwo.service.model.OrganizationModel;
import io.phasetwo.service.model.OrganizationPositionModel;
import io.phasetwo.service.representation.OrganizationPosition;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.admin.OperationType;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.stream.Stream;

import static io.phasetwo.service.resource.Converters.convertPositionModelToPosition;
import static io.phasetwo.service.resource.OrganizationResourceType.ORGANIZATION_POSITION;

@JBossLog
public class PositionsResource extends OrganizationAdminResource {

  private final OrganizationModel organization;

  protected PositionsResource(OrganizationAdminResource parent, OrganizationModel organization) {
    super(parent);
    this.organization = organization;
  }

  @GET
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  public Stream<OrganizationPosition> getTopPositions() {
    return organization.getTopPositionsStream().map(Converters::convertPositionModelToPosition);
  }

  @Path("{positionId}")
  public PositionResource getTopPosition(@PathParam("positionId") String positionId) {
    OrganizationPositionModel position = organization.getPositionById(positionId)
        .orElseThrow(NotFoundException::new);

    return new PositionResource(this, organization, position);
  }

  @POST
  @Path("")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createTopPosition(OrganizationPosition rep) {
    canManage();
    organization.getPositionByName(rep.getName())
        .ifPresent(it -> {
          throw new ClientErrorException(Response.Status.CONFLICT);
        });

    OrganizationPositionModel p = organization.addPosition(rep.getName(), null);
    p.setDisplayName(rep.getDisplayName());

    OrganizationPosition position = convertPositionModelToPosition(p);
    adminEvent
        .resource(ORGANIZATION_POSITION.name())
        .operation(OperationType.CREATE)
        .resourcePath(session.getContext().getUri(), position.getName())
        .representation(position)
        .success();

    return Response.created(
            session.getContext().getUri().getAbsolutePathBuilder().path(position.getId()).build())
        .build();
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
