using System.Net;
using System.Net.Http.Json;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
namespace FamilyFit.Tests;

public sealed class BootstrapTests
{
    private static string Configure(FamilyServer server)
    {
        var code = AuthService.Secret();
        server.Services.GetRequiredService<IConfiguration>()["FAMILYFIT_BOOTSTRAP_CODE"] = code;
        return code;
    }
    [Fact] public async Task First_Code_Creates_Admin_And_Browser_Session_Without_A_Fake_Phone()
    {
        using var server = new FamilyServer(); var code = Configure(server); using var browser = server.Browser();
        Assert.Contains("Code d’initialisation", await browser.GetStringAsync("/login"));
        var response = await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Marc", 58, 68));
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var actor = (await browser.GetFromJsonAsync<Actor>("/api/v1/me"))!;
        Assert.True(actor.IsAdmin); Assert.Equal(Guid.Empty, actor.DeviceId); Assert.NotNull(actor.SessionId);
        Assert.Equal(HttpStatusCode.OK, (await browser.GetAsync("/api/v1/family")).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.GetAsync("/api/v1/sync")).StatusCode);
        await using var scope = server.Services.CreateAsyncScope(); await using var db = await scope.ServiceProvider.GetRequiredService<IDbContextFactory<FamilyDb>>().CreateDbContextAsync();
        var admin = await db.Members.SingleAsync(); Assert.Equal("Marc", admin.Nickname); Assert.True(admin.IsAdmin);
        Assert.Empty(await db.Devices.ToListAsync()); Assert.Null((await db.BrowserSessions.SingleAsync()).DeviceId); Assert.Single(await db.AdminBootstraps.ToListAsync());
        var user = AuthService.BootstrapPrincipal(admin, actor.SessionId!.Value);
        var mediator = scope.ServiceProvider.GetRequiredService<IMediator>();
        Assert.True((await mediator.CreateInvitation(user, admin.Id)).Success);
        Assert.True((await mediator.CreateMemberLoginLink(user, admin.Id)).Success);
    }
    [Fact] public async Task Missing_Or_Short_Code_Leaves_The_Site_Blank_And_Rejects_Setup()
    {
        using var server = new FamilyServer(); using var browser = server.Browser();
        var config = server.Services.GetRequiredService<IConfiguration>();
        foreach(var value in new[] { "", "too-short" })
        {
            config["FAMILYFIT_BOOTSTRAP_CODE"] = value;
            Assert.DoesNotContain("admin-bootstrap", await browser.GetStringAsync("/login"));
            Assert.Equal(HttpStatusCode.Forbidden, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(value, "Marc"))).StatusCode);
        }
    }
    [Fact] public async Task Wrong_Code_And_Invalid_Profile_Do_Not_Consume_The_Initial_Code()
    {
        using var server = new FamilyServer(); var code = Configure(server); using var browser = server.Browser();
        Assert.Equal(HttpStatusCode.Forbidden, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(AuthService.Secret(), "Marc"))).StatusCode);
        Assert.Equal(HttpStatusCode.BadRequest, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "M", 0, 0))).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Marc", 58, 68))).StatusCode);
    }
    [Fact] public async Task Code_Is_One_Time_Even_After_Logout_Or_Environment_Rotation()
    {
        using var server = new FamilyServer(); var code = Configure(server); using var browser = server.Browser();
        Assert.Equal(HttpStatusCode.OK, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Marc"))).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await browser.PostAsync("/api/v1/auth/logout", null)).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.GetAsync("/api/v1/me")).StatusCode);
        Assert.Equal(HttpStatusCode.Conflict, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Autre"))).StatusCode);
        var replacement = Configure(server);
        Assert.Equal(HttpStatusCode.Conflict, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(replacement, "Autre"))).StatusCode);
        Assert.DoesNotContain("admin-bootstrap", await browser.GetStringAsync("/login"));
    }
    [Fact] public async Task Existing_Unconnected_Admin_Is_Reused_Without_Replacing_His_Profile()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true, "Déjà créé"); var code = Configure(server); using var browser = server.Browser();
        var html = await browser.GetStringAsync("/login"); Assert.Contains("Code d’initialisation", html); Assert.DoesNotContain("Ton pseudo", html);
        Assert.Equal(HttpStatusCode.OK, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Autre", 60, 100))).StatusCode);
        Assert.Equal(member.Id, (await browser.GetFromJsonAsync<Actor>("/api/v1/me"))!.MemberId);
    }
    [Fact] public async Task An_Already_Used_Installation_Cannot_Be_Bootstrapped()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); await server.ApprovedBrowserPrincipal(admin); var code = Configure(server); using var browser = server.Browser();
        Assert.DoesNotContain("admin-bootstrap", await browser.GetStringAsync("/login"));
        Assert.Equal(HttpStatusCode.Conflict, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code))).StatusCode);
    }
    [Fact] public async Task Concurrent_First_Connections_Create_Only_One_Administrator_And_Session()
    {
        using var server = new FamilyServer(); var code = Configure(server); using var first = server.Browser(); using var second = server.Browser();
        var answers = await Task.WhenAll(first.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Premier")), second.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Deuxième")));
        Assert.Single(answers, x => x.StatusCode == HttpStatusCode.OK); Assert.Single(answers, x => x.StatusCode == HttpStatusCode.Conflict);
        await using var scope = server.Services.CreateAsyncScope(); await using var db = await scope.ServiceProvider.GetRequiredService<IDbContextFactory<FamilyDb>>().CreateDbContextAsync();
        Assert.Equal(1, await db.Members.CountAsync()); Assert.Equal(1, await db.BrowserSessions.CountAsync());
    }
    [Fact] public async Task Setup_Rejects_Foreign_Origins_And_Rate_Limits_Wrong_Codes()
    {
        using var server = new FamilyServer(); var code = Configure(server); using var browser = server.Browser();
        browser.DefaultRequestHeaders.Add("Origin", "https://untrusted.example");
        Assert.Equal(HttpStatusCode.Forbidden, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Marc"))).StatusCode);
        browser.DefaultRequestHeaders.Remove("Origin");
        for(var i=0;i<4;i++) Assert.Equal(HttpStatusCode.Forbidden, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(AuthService.Secret(), "Marc"))).StatusCode);
        Assert.Equal(HttpStatusCode.TooManyRequests, (await browser.PostAsJsonAsync("/api/v1/auth/bootstrap", new BootstrapInput(code, "Marc"))).StatusCode);
    }
}
