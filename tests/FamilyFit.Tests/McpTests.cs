using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Contracts.Models.Workouts;
using FamilyFit.Core.Services;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using ModelContextProtocol.Client;
using ModelContextProtocol.Protocol;

namespace FamilyFit.Tests;

public sealed class McpTests
{
    private static object Rpc => new { jsonrpc = "2.0", id = 1, method = "tools/list", @params = new { } };
    private static JsonElement Data(CallToolResult result)
    {
        Assert.False(result.IsError == true, string.Join(" ", result.Content.OfType<TextContentBlock>().Select(x => x.Text)));
        return result.StructuredContent!.Value.GetProperty("data");
    }
    private static async Task<McpClient> Connect(FamilyServer server, HttpClient http, string protocol = "2026-07-28")
    {
        http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", server.McpKey);
        var transport = new HttpClientTransport(new HttpClientTransportOptions { Endpoint = new Uri("https://localhost/mcp") }, http);
        return await McpClient.CreateAsync(transport, new McpClientOptions { ProtocolVersion = protocol, ClientInfo = new Implementation { Name = "FamilyFit.Tests", Version = "1.0" } });
    }

    [Theory] [InlineData("2025-11-25")] [InlineData("2026-07-28")]
    public async Task Real_Mcp_Client_Discovers_All_Tools_Without_Client_Supplied_Identity(string protocol)
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http, protocol);
        var tools = await client.ListToolsAsync();
        Assert.Equal(18, tools.Count);
        Assert.Contains(tools, x => x.Name == "get_family_dashboard"); Assert.Contains(tools, x => x.Name == "publish_program");
        Assert.DoesNotContain(tools, x => x.Name.Contains("key") || x.Name.Contains("invitation") || x.Name.Contains("login"));
        foreach (var tool in tools) { var schema = tool.JsonSchema.GetProperty("properties"); Assert.False(schema.TryGetProperty("user", out _)); Assert.False(schema.TryGetProperty("claimsPrincipal", out _)); }
        var identity = Data(await client.CallToolAsync("get_current_member")); Assert.Equal(admin.Id, identity.GetProperty("memberId").GetGuid());
        Assert.DoesNotContain(server.McpKey, JsonSerializer.Serialize(tools.Select(x => x.ProtocolTool)));
    }

    [Fact] public async Task Mcp_Rejects_Anonymous_Wrong_Mobile_And_Browser_Credentials()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var anonymous = server.Browser();
        Assert.Equal(HttpStatusCode.Unauthorized, (await anonymous.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
        anonymous.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", new string('x', 64));
        Assert.Equal(HttpStatusCode.Unauthorized, (await anonymous.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
        var (phone, _) = await server.Phone(admin);
        Assert.Equal(HttpStatusCode.Unauthorized, (await phone.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
        using var browser = server.Browser(); var challenge = await server.Challenge(browser, admin);
        await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(challenge.GetProperty("code").GetString()!)); await browser.PostAsync("/api/v1/auth/browser/consume", null);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
        phone.Dispose();
    }

    [Fact] public async Task Missing_Or_Removed_Server_Key_Disables_Mcp_And_Rotation_Invalidates_Old_Principals()
    {
        using var server = new FamilyServer(); await server.AddMember(true); using var http = server.Browser();
        var configuration = server.Services.GetRequiredService<IConfiguration>();
        await using var scope = server.Services.CreateAsyncScope(); var authorization = scope.ServiceProvider.GetRequiredService<AgentAuthorization>();
        var principal = (await authorization.AuthenticateAsync(server.McpKey))!;
        configuration["MCP_API_KEY"] = AuthService.Secret();
        await Assert.ThrowsAsync<DomainException>(() => authorization.RequireActorAsync(principal, false, default));
        http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", server.McpKey);
        Assert.Equal(HttpStatusCode.Unauthorized, (await http.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
        configuration["MCP_API_KEY"] = "";
        Assert.False(authorization.IsConfigured);
        Assert.Equal(HttpStatusCode.Unauthorized, (await http.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
    }

    [Fact] public async Task Read_Only_Mode_Rejects_Changes_While_Paginated_Reads_Still_Work()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); await server.AddMember(name: "Deuxième"); using var http = server.Browser();
        server.Services.GetRequiredService<IConfiguration>()["MCP_READ_ONLY"] = "true";
        await using var client = await Connect(server, http);
        var page = Data(await client.CallToolAsync("list_members", new Dictionary<string, object?> { ["take"] = 1 }));
        Assert.Single(page.GetProperty("items").EnumerateArray()); Assert.True(page.GetProperty("hasMore").GetBoolean());
        var input = new MemberInput("Changé", 30, 71, "Forme", "Débutant", 30, "1,3,5", "", admin.Revision);
        var denied = await client.CallToolAsync("save_member", new Dictionary<string, object?> { ["memberId"] = admin.Id, ["input"] = input });
        Assert.True(denied.IsError);
        Assert.True((await client.CallToolAsync("list_members", new Dictionary<string, object?> { ["take"] = 101 })).IsError);
    }

    [Fact] public async Task Mcp_Uses_The_Real_Program_And_Result_Rules_Over_Http()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); using var http = server.Browser(); await using var client = await Connect(server, http);
        var location = Data(await client.CallToolAsync("save_location", new Dictionary<string, object?> { ["input"] = new LocationInput(admin.Id, "Maison MCP", ["BODYWEIGHT"]) })).Deserialize<Location>(JsonDefaults.Options)!;
        var catalogue = Data(await client.CallToolAsync("list_exercises", new Dictionary<string, object?> { ["equipmentId"] = "BODYWEIGHT" }));
        var exerciseId = catalogue.GetProperty("items")[0].GetProperty("id").GetString()!;
        var monday = FamilyCalendar.Monday(FamilyCalendar.Today(server.Clock));
        var input = new ProgramInput(admin.Id, "Préparé avec MCP", monday, Enumerable.Range(0, 7).Select(i => new ProgramDayInput(monday.AddDays(i), "Gainage", location.Id, 20, false, [new Prescription(exerciseId, 3, 12, 20)])).ToList());
        var draft = Data(await client.CallToolAsync("save_program_draft", new Dictionary<string, object?> { ["input"] = input })).Deserialize<WorkoutProgram>(JsonDefaults.Options)!;
        var published = Data(await client.CallToolAsync("publish_program", new Dictionary<string, object?> { ["programId"] = draft.Id })).Deserialize<WorkoutProgram>(JsonDefaults.Options)!;
        Assert.Equal("published", published.State);
        Assert.True((await client.CallToolAsync("save_program_draft", new Dictionary<string, object?> { ["programId"] = draft.Id, ["input"] = input })).IsError);
        var mondayDay = published.Days.Single(x => x.Date == monday);
        var submission = server.Submission(published) with { ProgramDayId = mondayDay.Id };
        var arguments = new Dictionary<string, object?> { ["input"] = submission };
        var first = Data(await client.CallToolAsync("record_workout_result", arguments)); var second = Data(await client.CallToolAsync("record_workout_result", arguments));
        Assert.Equal(first.GetProperty("id").GetGuid(), second.GetProperty("id").GetGuid()); Assert.Equal("completed", first.GetProperty("status").GetString());
        var dashboard = Data(await client.CallToolAsync("get_family_dashboard", new Dictionary<string, object?> { ["date"] = monday }));
        Assert.Equal(1, dashboard.GetProperty("planned").GetInt32()); Assert.Equal(1, dashboard.GetProperty("completed").GetInt32());
        var detail = Data(await client.CallToolAsync("get_workout_result", new Dictionary<string, object?> { ["resultId"] = submission.Id }));
        Assert.Equal(submission.ProgramDayId, detail.GetProperty("prescription").GetProperty("id").GetGuid());
        var planning = Data(await client.CallToolAsync("get_planning", new Dictionary<string, object?> { ["from"] = monday, ["to"] = monday.AddDays(6) }));
        Assert.Equal(7, planning.GetProperty("items").GetArrayLength());
    }

    [Fact] public async Task An_Agent_Cannot_Approve_Login_Manage_Credentials_Or_Use_Mobile_Sync()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true);
        await using var scope = server.Services.CreateAsyncScope(); var auth = scope.ServiceProvider.GetRequiredService<AgentAuthorization>(); var mediator = scope.ServiceProvider.GetRequiredService<IMediator>();
        var principal = (await auth.AuthenticateAsync(server.McpKey))!;
        Assert.Equal(403, (await mediator.CreateMemberLoginLink(principal, admin.Id)).Status);
        Assert.Equal(403, (await mediator.CreateInvitation(principal, admin.Id)).Status);
        Assert.Equal(403, (await mediator.GetSync(principal)).Status);
        Assert.Equal(403, (await mediator.ApproveBrowserLogin(principal, "ABCDEFGHJK")).Status);
        using var http = server.Browser(); http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", server.McpKey);
        Assert.Equal(HttpStatusCode.Unauthorized, (await http.GetAsync("/api/v1/sync")).StatusCode);
    }

    [Fact] public async Task Foreign_Origins_Are_Rejected_Even_With_The_Valid_Server_Key()
    {
        using var server = new FamilyServer(); await server.AddMember(true); using var http = server.Browser();
        http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", server.McpKey); http.DefaultRequestHeaders.Add("Origin", "https://untrusted.example");
        Assert.Equal(HttpStatusCode.Forbidden, (await http.PostAsJsonAsync("/mcp", Rpc)).StatusCode);
    }
}
