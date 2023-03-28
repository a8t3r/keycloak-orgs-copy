package io.phasetwo.service.resource;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import io.phasetwo.client.OrganizationResource;
import io.phasetwo.client.OrganizationsResource;
import io.phasetwo.client.UserResource;
import io.phasetwo.client.*;
import io.phasetwo.client.openapi.model.OrganizationPositionHierarchyRepresentation;
import io.phasetwo.client.openapi.model.OrganizationPositionRepresentation;
import io.phasetwo.client.openapi.model.OrganizationRoleRepresentation;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.phasetwo.service.Helpers.createUser;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

@SuppressWarnings("UnstableApiUsage")
public class PositionsTest extends AbstractResourceTest {

  private PhaseTwo client;
  private OrganizationsResource organizations;

  private OrganizationPositionsResource positions;
  private String orgId;

  @Before
  public void init() {
    this.client = phaseTwo();
    this.organizations = client.organizations(REALM);
    this.orgId = createDefaultOrg(organizations);
    this.positions = organizations.organization(orgId).positions();
  }

  @After
  public void close() {
    organizations.organization(orgId).delete();
  }

  private OrganizationPositionResource createPosition() {
    String positionId = positions.createTop(new OrganizationPositionRepresentation().name("position"));
    return positions.position(positionId);
  }

  private Map<String, String> createPositionHierarchy(Graph<String> hierarchy) {
    // name to id index
    Map<String, String> positionsIndex = hierarchy.nodes().stream().collect(Collectors.toMap(
        Function.identity(),
        it -> positions.createTop(new OrganizationPositionRepresentation().name(it))
    ));

    hierarchy.edges().forEach(it -> positions.position(positionsIndex.get(it.source()))
        .addSubordinate(new OrganizationPositionRepresentation().name(it.target())));

    return positionsIndex;
  }

  @Test
  public void testPositionUsersPagination() {
    String user1Id = createUser(server.client(), REALM, "johndoe").getId();
    String user2Id = createUser(server.client(), REALM, "janedoe").getId();
    organizations.organization(orgId).memberships().add(user1Id);
    organizations.organization(orgId).memberships().add(user2Id);

    OrganizationPositionResource positionResource = createPosition();
    positionResource.assignUser(user1Id);
    positionResource.assignUser(user2Id);

    List<io.phasetwo.client.openapi.model.UserRepresentation> users = positionResource.users();
    assertThat(users, hasSize(2));

    users = positionResource.users(0, 1);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getUsername(), is("johndoe"));

    users = positionResource.users(1, 1);
    assertThat(users, hasSize(1));
    assertThat(users.get(0).getUsername(), is("janedoe"));
  }

  @Test
  public void testUserAssign() {
    OrganizationPositionResource positionResource = createPosition();
    String userId = createUser(server.client(), REALM, "johndoe").getId();

    assertThat(positionResource.isAssignedUser(userId), is(false));
    assertThrows(ClientErrorException.class, () -> positionResource.assignUser(userId));

    organizations.organization(orgId).memberships().add(userId);
    assertThat(positionResource.isAssignedUser(userId), is(false));

    positionResource.assignUser(userId);
    assertThat(positionResource.isAssignedUser(userId), is(true));
    positionResource.assignUser(userId);

    positionResource.removeUser(userId);
    assertThat(positionResource.isAssignedUser(userId), is(false));
  }

  @Test
  public void testPositionLifecycle() {
    String positionId = createPosition().get().getId();
    List<OrganizationPositionRepresentation> list = positions.getTop();
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getId(), equalTo(positionId));
    assertThat(list.get(0).getName(), equalTo("position"));

    OrganizationPositionResource positionResource = positions.position(positionId);
    OrganizationPositionRepresentation position = positionResource.get();
    assertThat(position.getId(), equalTo(positionId));
    assertThat(position.getName(), equalTo("position"));
    assertThat(position.getDisplayName(), nullValue());

    positionResource.update(new OrganizationPositionRepresentation().name("updated").displayName("displayName"));
    position = positionResource.get();
    assertThat(position.getId(), equalTo(positionId));
    assertThat(position.getName(), equalTo("updated"));
    assertThat(position.getDisplayName(), equalTo("displayName"));

    positionResource.delete();
    assertThat(positions.getTop(), empty());
    assertThrows(NotFoundException.class, () -> positions.position(positionId).get());
  }

  @Test
  public void testShouldNotBeSubordinatedByItself() {
    String positionId = positions.createTop(new OrganizationPositionRepresentation().name("a"));

    ClientErrorException ex = assertThrows(ClientErrorException.class,
        () -> positions.position(positionId).addSubordinate(new OrganizationPositionRepresentation().name("a")));
    assertThat(ex.getResponse().getStatus(), is(HttpStatus.SC_BAD_REQUEST));

    ex = assertThrows(ClientErrorException.class,
        () -> positions.position(positionId).addSubordinate(new OrganizationPositionRepresentation().id(positionId)));
    assertThat(ex.getResponse().getStatus(), is(HttpStatus.SC_BAD_REQUEST));
  }

  @Test
  public void testPositionWithRoles() {
    OrganizationRolesResource rolesResource = organizations.organization(orgId).roles();
    rolesResource.create(new OrganizationRoleRepresentation().name("eat-fruits"));
    rolesResource.create(new OrganizationRoleRepresentation().name("eat-meat"));

    OrganizationPositionResource positionResource = createPosition();
    assertThat(positionResource.directRoles(), empty());

    positionResource.grantRole("eat-fruits");
    assertThat(positionResource.directRoles(), hasSize(1));

    positionResource.grantRole("eat-meat");
    assertThat(positionResource.directRoles(), hasSize(2));

    assertThrows(NotFoundException.class, () -> positionResource.grantRole("unknown-role"));
    assertThat(positionResource.directRoles(), hasSize(2));

    rolesResource.delete("eat-fruits");
    assertThat(positionResource.directRoles(), hasSize(1));

    positionResource.revokeRole("eat-meat");
    assertThat(positionResource.directRoles(), empty());
  }

  @Test
  public void testPositionWithRolesAndUsers() {
    OrganizationResource organizationResource = organizations.organization(orgId);
    OrganizationRolesResource rolesResource = organizationResource.roles();
    rolesResource.create(new OrganizationRoleRepresentation().name("eat-fruits"));
    OrganizationPositionResource positionResource = createPosition();
    UserRepresentation user = createUser(server.client(), REALM, "johndoe");
    positionResource.grantRole("eat-fruits");

    ClientErrorException ex = assertThrows(ClientErrorException.class, () -> positionResource.assignUser(user.getId()));
    assertThat(ex.getResponse().getStatus(), is(HttpStatus.SC_BAD_REQUEST));

    organizationResource.memberships().add(user.getId());
    positionResource.assignUser(user.getId());

    assertThat(positionResource.directRoles(), hasSize(1));
    assertThat(positionResource.users(), hasSize(1));

    UserResource userResource = client.users(REALM).user(user.getId());
    List<OrganizationRoleRepresentation> assignedRoles = userResource.getRoles(orgId);
    assertThat(assignedRoles, hasSize(1));

    organizationResource.memberships().remove(user.getId());
    assertThat(positionResource.users(), empty());
  }

  @Test
  public void testHierarchyWithCycle() {
    OrganizationResource organizationResource = organizations.organization(orgId);
    OrganizationRolesResource rolesResource = organizationResource.roles();

    rolesResource.create(new OrganizationRoleRepresentation().name("eat-fruits"));
    String fruitEaterId = positions.createTop(new OrganizationPositionRepresentation().name("Fruit-eaters"));
    OrganizationPositionResource fruitEatersResource = positions.position(fruitEaterId);
    fruitEatersResource.grantRole("eat-fruits");

    rolesResource.create(new OrganizationRoleRepresentation().name("eat-apples"));
    String appleEaterId = positions.createTop(new OrganizationPositionRepresentation().name("Apple-eaters"));
    OrganizationPositionResource appleEatersResource = positions.position(appleEaterId);
    appleEatersResource.grantRole("eat-apples");

    UserRepresentation user = createUser(server.client(), REALM, "johndoe");
    organizationResource.memberships().add(user.getId());
    UserResource userResource = client.users(REALM).user(user.getId());

    fruitEatersResource.assignUser(user.getId());
    List<OrganizationRoleRepresentation> roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(1));
    assertThat(roles, everyItem(hasProperty("name", is("eat-fruits"))));

    appleEatersResource.assignUser(user.getId());
    roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(2));
    assertThat(roles, everyItem(hasProperty("name", oneOf("eat-fruits", "eat-apples"))));

    fruitEatersResource.removeUser(user.getId());
    appleEatersResource.removeUser(user.getId());
    roles = userResource.getRoles(orgId);
    assertThat(roles, empty());

    // Make cycle between positions
    fruitEatersResource.addSubordinate(new OrganizationPositionRepresentation().name("Apple-eaters"));
    appleEatersResource.addSubordinate(new OrganizationPositionRepresentation().name("Fruit-eaters"));

    appleEatersResource.assignUser(user.getId());
    roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(2));
    assertThat(roles, everyItem(hasProperty("name", oneOf("eat-fruits", "eat-apples"))));
    appleEatersResource.removeUser(user.getId());

    fruitEatersResource.assignUser(user.getId());
    roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(2));
    assertThat(roles, everyItem(hasProperty("name", oneOf("eat-fruits", "eat-apples"))));
    fruitEatersResource.removeUser(user.getId());

    OrganizationPositionHierarchyRepresentation hierarchy = fruitEatersResource.hierarchy();
    assertThat(hierarchy, notNullValue());
    assertThat(hierarchy.getName(), is("Fruit-eaters"));
    assertThat(hierarchy.getSubordinates(), hasSize(1));
    assertThat(hierarchy.getSubordinates().get(0).getName(), is("Apple-eaters"));
    assertThat(hierarchy.getSubordinates().get(0).getSubordinates(), hasSize(1));
    assertThat(hierarchy.getSubordinates().get(0).getSubordinates().get(0).getName(), is("Fruit-eaters"));
    assertThat(hierarchy.getSubordinates().get(0).getSubordinates().get(0).getSubordinates(), empty());
  }

  @Test
  public void testMultiLevelHierarchy() {
    OrganizationResource organizationResource = organizations.organization(orgId);
    OrganizationRolesResource rolesResource = organizationResource.roles();

    rolesResource.create(new OrganizationRoleRepresentation().name("publish-reports"));
    rolesResource.create(new OrganizationRoleRepresentation().name("publish-issue"));

    // create hierarchy:
    // CEO -> CTO -> Product Manager -> Developer
    Map<String, String> positionsIndex = createPositionHierarchy(GraphBuilder.directed()
            .<String>immutable()
            .addNode("CEO")
            .addNode("CTO")
            .addNode("Product Manager")
            .addNode("Developer")
            .putEdge("CEO", "CTO")
            .putEdge("CTO", "Product Manager")
            .putEdge("Product Manager", "Developer")
            .build()
    );

    OrganizationPositionResource developerPositionResource = positions.position(positionsIndex.get("Developer"));
    developerPositionResource.grantRole("publish-issue");

    OrganizationPositionResource ctoPositionResource = positions.position(positionsIndex.get("CTO"));
    ctoPositionResource.grantRole("publish-reports");

    UserRepresentation user = createUser(server.client(), REALM, "johndoe");
    organizationResource.memberships().add(user.getId());
    UserResource userResource = client.users(REALM).user(user.getId());
    positions.position(positionsIndex.get("CEO")).assignUser(user.getId());

    List<OrganizationRoleRepresentation> roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(2));
    assertThat(roles, everyItem(hasProperty("name", oneOf("publish-reports", "publish-issue"))));
  }

  @Test
  public void testIncludeInherited() {
    OrganizationResource organizationResource = organizations.organization(orgId);
    OrganizationRolesResource rolesResource = organizationResource.roles();

    rolesResource.create(new OrganizationRoleRepresentation().name("publish-reports"));
    rolesResource.create(new OrganizationRoleRepresentation().name("publish-issue"));
    rolesResource.create(new OrganizationRoleRepresentation().name("close-issue"));

    // create hierarchy:
    // CTO -> Product Manager, QA Manager
    // Product Manager -> Developer
    Map<String, String> positionsIndex = createPositionHierarchy(GraphBuilder.directed()
        .<String>immutable()
        .addNode("CTO")
        .addNode("Product Manager")
        .addNode("Developer")
        .addNode("QA Manager")
        .putEdge("CTO", "Product Manager")
        .putEdge("CTO", "QA Manager")
        .putEdge("Product Manager", "Developer")
        .build()
    );

    // Grant roles to positions
    OrganizationPositionResource developer = positions.position(positionsIndex.get("Developer"));
    developer.grantRole("publish-issue");

    OrganizationPositionResource qaManagerResource = positions.position(positionsIndex.get("QA Manager"));
    qaManagerResource.grantRole("close-issue");

    OrganizationPositionResource productManagerResource = positions.position(positionsIndex.get("Product Manager"));
    productManagerResource.grantRole("publish-reports");

    OrganizationPositionResource ctoPosition = positions.position(positionsIndex.get("CTO"));
    assertThat(ctoPosition.directRoles(), empty());
    assertThat(ctoPosition.allRoles(), hasSize(3));
    assertThat(ctoPosition.allRoles(), everyItem(hasProperty("name",
        oneOf("publish-reports", "publish-issue", "close-issue"))));

    assertThat(ctoPosition.directSubordinates(), hasSize(2));
    assertThat(ctoPosition.directSubordinates(), everyItem(hasProperty("name",
        oneOf("Product Manager", "QA Manager"))));
    assertThat(ctoPosition.allSubordinates(), hasSize(3));
    assertThat(ctoPosition.allSubordinates(), everyItem(hasProperty("name",
        oneOf("Product Manager", "QA Manager", "Developer"))));
  }

  @Test
  public void testMultiLevelHierarchyWithBranches() {
    OrganizationResource organizationResource = organizations.organization(orgId);
    OrganizationRolesResource rolesResource = organizationResource.roles();

    rolesResource.create(new OrganizationRoleRepresentation().name("publish-reports"));
    rolesResource.create(new OrganizationRoleRepresentation().name("publish-issue"));
    rolesResource.create(new OrganizationRoleRepresentation().name("close-issue"));

    // create hierarchy:
    // CTO -> Product Manager, QA Manager
    // Product Manager -> Developer
    Map<String, String> positionsIndex = createPositionHierarchy(GraphBuilder.directed()
        .<String>immutable()
        .addNode("CTO")
        .addNode("Product Manager")
        .addNode("Developer")
        .addNode("QA Manager")
        .putEdge("CTO", "Product Manager")
        .putEdge("CTO", "QA Manager")
        .putEdge("Product Manager", "Developer")
        .build()
    );

    // Grant roles to positions
    OrganizationPositionResource developer = positions.position(positionsIndex.get("Developer"));
    developer.grantRole("publish-issue");

    OrganizationPositionResource qaManagerResource = positions.position(positionsIndex.get("QA Manager"));
    qaManagerResource.grantRole("close-issue");

    OrganizationPositionResource productManagerResource = positions.position(positionsIndex.get("Product Manager"));
    productManagerResource.grantRole("publish-reports");

    OrganizationPositionHierarchyRepresentation hierarchy = positions.position(positionsIndex.get("CTO")).hierarchy();
    assertThat(hierarchy, notNullValue());
    assertThat(hierarchy.getName(), is("CTO"));
    assertThat(hierarchy.getSubordinates(), hasSize(2));
    assertThat(hierarchy.getSubordinates().get(0).getName(), is("Product Manager"));
    assertThat(hierarchy.getSubordinates().get(1).getName(), is("QA Manager"));
    assertThat(hierarchy.getSubordinates().get(0).getSubordinates(), hasSize(1));

    UserRepresentation user = createUser(server.client(), REALM, "johndoe");
    organizationResource.memberships().add(user.getId());
    UserResource userResource = client.users(REALM).user(user.getId());

    // Assign user to CTO position
    OrganizationPositionResource ctoPosition = positions.position(positionsIndex.get("CTO"));
    ctoPosition.assignUser(user.getId());
    List<OrganizationRoleRepresentation> roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(3));
    assertThat(roles, everyItem(hasProperty("name",
        oneOf("publish-reports", "publish-issue", "close-issue"))));
    ctoPosition.removeUser(user.getId());

    // Assign user to Product Manager position
    OrganizationPositionResource productManagerPosition = positions.position(positionsIndex.get("Product Manager"));
    productManagerPosition.assignUser(user.getId());
    roles = userResource.getRoles(orgId);
    assertThat(roles, hasSize(2));
    assertThat(roles, everyItem(hasProperty("name",
        oneOf("publish-reports", "publish-issue"))));
    productManagerPosition.removeUser(user.getId());
  }

  @Test
  public void testPositionDeletion() {
    // create hierarchy:
    // CEO -> CTO -> Product Manager -> Developer
    Map<String, String> positionsIndex = createPositionHierarchy(GraphBuilder.directed()
        .<String>immutable()
        .addNode("CEO")
        .addNode("CTO")
        .addNode("Product Manager")
        .addNode("Developer")
        .putEdge("CEO", "CTO")
        .putEdge("CTO", "Product Manager")
        .putEdge("Product Manager", "Developer")
        .build()
    );

    List<OrganizationPositionRepresentation> topPositions = positions.getTop();
    assertThat(topPositions, hasSize(1));
    assertThat(topPositions, everyItem(hasProperty("name", is("CEO"))));

    positions.position(positionsIndex.get("CTO")).delete();
    topPositions = positions.getTop();
    assertThat(topPositions, hasSize(1));
    assertThat(topPositions, everyItem(hasProperty("name", oneOf("CEO", "Product Manager"))));
  }
}
