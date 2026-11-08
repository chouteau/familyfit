using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Equipment;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using ModelContextProtocol.Client;
using ModelContextProtocol.Protocol;

namespace FamilyFit.Tests;

public sealed class EquipmentTests
{
    private sealed record EquipmentView(string Id, string Name, string Description, string[] TargetMuscles, string? ImageUrl, long Revision);

    private const string Png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aYz8AAAAASUVORK5CYII=";

    private static async Task<McpClient> Connect(FamilyServer server, HttpClient http)
    {
        http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", server.McpKey);
        return await McpClient.CreateAsync(new HttpClientTransport(new HttpClientTransportOptions { Endpoint = new Uri("https://localhost/mcp") }, http));
    }

    private static JsonElement Data(CallToolResult result)
    {
        Assert.False(result.IsError == true, string.Join(" ", result.Content.OfType<TextContentBlock>().Select(x => x.Text)));
        return result.StructuredContent!.Value.GetProperty("data");
    }
    private static int ErrorStatus(CallToolResult result)
    {
        Assert.True(result.IsError);
        return JsonDocument.Parse(result.Content.OfType<TextContentBlock>().Single().Text).RootElement.GetProperty("status").GetInt32();
    }
    private static async Task<EquipmentView> Find(McpClient client, string id = "LEG_PRESS")
        => Data(await client.CallToolAsync("list_equipment")).Deserialize<List<EquipmentView>>(JsonDefaults.Options)!.Single(x => x.Id == id);
    private static async Task<CallToolResult> Photo(McpClient client, long revision, string? image = Png, string? type = "image/png", bool remove = false)
        => await client.CallToolAsync("set_equipment_image", new Dictionary<string, object?> { ["equipmentId"] = "LEG_PRESS", ["input"] = new EquipmentImageInput(revision, image, type, remove) });

    [Fact]
    public async Task Catalogue_Seeds_Existing_Identifiers_And_Mcp_Can_Create_Declare_And_Update_Equipment()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var catalogue = Data(await client.CallToolAsync("list_equipment")).Deserialize<List<EquipmentView>>(JsonDefaults.Options)!;
        Assert.Equal(EquipmentCatalogue.All.Keys.Order(), catalogue.Select(x => x.Id).Order());
        Assert.All(catalogue, x => { Assert.NotEmpty(x.Description); Assert.NotEmpty(x.TargetMuscles); Assert.Null(x.ImageUrl); });
        var input = new EquipmentInput("ROWER", "Rameur", "Poussée des jambes et tirage des bras.", ["Cardio", "Dos"]);
        var created = Data(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = input })).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.Equal(1, created.Revision);
        Assert.Equal(409, ErrorStatus(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = input })));
        var location = Data(await client.CallToolAsync("save_location", new Dictionary<string, object?> { ["input"] = new LocationInput(admin.Id, "Maison", ["ROWER"]) })).Deserialize<Location>(JsonDefaults.Options)!;
        Assert.Contains("ROWER", FamilyCalendar.Split(location.EquipmentIds)); Assert.Contains("BODYWEIGHT", FamilyCalendar.Split(location.EquipmentIds));
        input = input with { Revision = created.Revision, Description = "Description enrichie", TargetMuscles = [" Dos ", "dos", "Cardio"] };
        var updated = Data(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = input })).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.Equal(2, updated.Revision); Assert.Equal(new[] { "Dos", "Cardio" }, updated.TargetMuscles);
        Assert.Equal(409, ErrorStatus(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = input })));
        Assert.Equal(400, ErrorStatus(await client.CallToolAsync("save_location", new Dictionary<string, object?> { ["input"] = new LocationInput(admin.Id, "Salle", ["UNKNOWN"]) })));
    }

    [Fact]
    public async Task Photo_Upload_Replace_And_Removal_Are_Private_Persistent_And_Revision_Protected()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var before = await Find(client);
        var uploaded = Data(await Photo(client, before.Revision)).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.NotNull(uploaded.ImageUrl); Assert.Equal(before.Revision + 1, uploaded.Revision);
        using var anonymous = server.Browser(); Assert.Equal(HttpStatusCode.Unauthorized, (await anonymous.GetAsync(uploaded.ImageUrl)).StatusCode);
        var (phone, _) = await server.Phone(admin); using var authenticated = phone;
        var response = await authenticated.GetAsync(uploaded.ImageUrl);
        Assert.Equal(HttpStatusCode.OK, response.StatusCode); Assert.Equal("image/png", response.Content.Headers.ContentType!.MediaType);
        Assert.Equal(Convert.FromBase64String(Png), await response.Content.ReadAsByteArrayAsync());
        Assert.True(response.Headers.CacheControl!.NoStore); Assert.Equal("nosniff", response.Headers.GetValues("X-Content-Type-Options").Single());
        Assert.DoesNotContain(Png, Data(await client.CallToolAsync("list_equipment")).GetRawText());
        Assert.Equal(409, ErrorStatus(await Photo(client, before.Revision)));
        var metadata = new EquipmentInput(uploaded.Id, uploaded.Name, "Description gardée au redémarrage", ["Quadriceps"], uploaded.Revision);
        var saved = Data(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = metadata })).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.Equal(uploaded.ImageUrl, saved.ImageUrl);
        await using (var scope = server.Services.CreateAsyncScope())
        {
            await using var db = await scope.ServiceProvider.GetRequiredService<IDbContextFactory<FamilyDb>>().CreateDbContextAsync();
            await CatalogueSeeder.SeedAsync(db); await CatalogueSeeder.SeedAsync(db);
            var persisted = await db.Equipment.AsNoTracking().SingleAsync(x => x.Id == saved.Id);
            Assert.Equal(saved.Description, persisted.Description); Assert.Equal(saved.ImageUrl, persisted.ImageUrl); Assert.Equal(saved.Revision, persisted.Revision);
        }
        var replaced = Data(await Photo(client, saved.Revision)).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.NotEqual(saved.ImageUrl, replaced.ImageUrl);
        var removed = Data(await Photo(client, replaced.Revision, null, null, true)).Deserialize<EquipmentView>(JsonDefaults.Options)!;
        Assert.Null(removed.ImageUrl); Assert.Null((await Find(client)).ImageUrl);
        Assert.Equal(HttpStatusCode.NotFound, (await authenticated.GetAsync(uploaded.ImageUrl)).StatusCode);
    }

    [Theory]
    [InlineData("not base64", "image/png")]
    [InlineData("PHN2ZyBvbmxvYWQ9J2FsZXJ0KDEpJz48L3N2Zz4=", "image/svg+xml")]
    [InlineData(Png, "image/jpeg")]
    [InlineData("data:image/png;base64," + Png, "image/png")]
    public async Task Invalid_Photos_Are_Rejected_Without_Changing_The_Equipment(string image, string type)
    {
        using var server = new FamilyServer(); await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var before = await Find(client);
        Assert.Equal(400, ErrorStatus(await Photo(client, before.Revision, image, type)));
        var after = await Find(client); Assert.Equal(before.Revision, after.Revision); Assert.Null(after.ImageUrl);
    }

    [Fact]
    public async Task Oversized_And_Ambiguous_Removal_Payloads_Are_Rejected()
    {
        using var server = new FamilyServer(); await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var before = await Find(client);
        Assert.Equal(400, ErrorStatus(await Photo(client, before.Revision, Convert.ToBase64String(new byte[2097153]))));
        Assert.Equal(400, ErrorStatus(await Photo(client, before.Revision, remove: true)));
        Assert.Equal(before.Revision, (await Find(client)).Revision);
    }

    [Fact]
    public async Task Read_Only_Mcp_And_Human_Access_Cannot_Change_The_Catalogue()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var equipment = await Find(client);
        var input = new EquipmentInput(equipment.Id, equipment.Name, "Modification", ["Jambes"], equipment.Revision);
        server.Services.GetRequiredService<IConfiguration>()["MCP_READ_ONLY"] = "true";
        Assert.Equal(403, ErrorStatus(await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = input })));
        Assert.Equal(403, ErrorStatus(await Photo(client, equipment.Revision)));
        Assert.Equal(equipment.Revision, (await Find(client)).Revision);
        var principal = await server.ApprovedBrowserPrincipal(admin);
        await using var scope = server.Services.CreateAsyncScope(); var mediator = scope.ServiceProvider.GetRequiredService<IMediator>();
        Assert.True((await mediator.GetEquipmentCatalogue(principal)).Success);
        Assert.Equal(403, (await mediator.SaveEquipment(principal, input)).Status);
        Assert.Equal(403, (await mediator.SaveEquipmentImage(principal, equipment.Id, new EquipmentImageInput(equipment.Revision, Png, "image/png"))).Status);
    }

    [Theory]
    [InlineData("bad/id", "Nom", "Description")]
    [InlineData("VALID", " ", "Description")]
    [InlineData("VALID", "Nom", " ")]
    public async Task Invalid_Metadata_Is_Rejected(string id, string name, string description)
    {
        using var server = new FamilyServer(); await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var result = await client.CallToolAsync("save_equipment", new Dictionary<string, object?> { ["input"] = new EquipmentInput(id, name, description, ["Jambes"]) });
        Assert.Equal(400, ErrorStatus(result));
    }
}
