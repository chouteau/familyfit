using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;
using Microsoft.Extensions.DependencyInjection;

namespace FamilyFit.Tests;

public sealed class PrivateLoginTests
{
    [Fact] public async Task Anonymous_Login_Is_Blank_And_Cannot_Create_A_Challenge()
    {
        using var server = new FamilyServer(); using var browser = server.Browser();
        var page = await browser.GetStringAsync("/login");
        Assert.DoesNotContain("data-login", page); Assert.DoesNotContain("QR code de connexion", page); Assert.DoesNotContain("Ton mobile ouvre", page);
        Assert.Equal(HttpStatusCode.BadRequest, (await browser.PostAsync("/api/v1/auth/browser/challenges", null)).StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, (await browser.PostAsJsonAsync("/api/v1/auth/browser/challenges", new BrowserChallengeInput("unknown"))).StatusCode);
        Assert.Equal("no-referrer", (await browser.GetAsync("/login")).Headers.GetValues("Referrer-Policy").Single());
    }
    [Fact] public async Task Link_Is_Bound_To_The_Member_And_Expires_Exactly_After_Five_Minutes()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var link = await server.LoginLink(member);
        Assert.Equal(server.Clock.Now.AddMinutes(5), link.ExpiresAtUtc);
        Assert.Empty(new Uri(link.Url).Query); Assert.StartsWith("#secret=", new Uri(link.Url).Fragment);
        await using var scope = server.Services.CreateAsyncScope(); var auth = scope.ServiceProvider.GetRequiredService<AuthService>();
        var preview = await auth.PreviewMemberLoginLinkAsync(FamilyServer.LinkSecret(link), null);
        Assert.Equal(member.Id, preview!.MemberId); Assert.Equal(member.Nickname, preview.Nickname);
        server.Clock.Now = link.ExpiresAtUtc;
        Assert.Null(await auth.PreviewMemberLoginLinkAsync(FamilyServer.LinkSecret(link), null));
        using var browser = server.Browser();
        Assert.Equal(HttpStatusCode.NotFound, (await browser.PostAsJsonAsync("/api/v1/auth/browser/challenges", new BrowserChallengeInput(FamilyServer.LinkSecret(link)))).StatusCode);
    }
    [Fact] public async Task A_Different_Phone_Cannot_Approve_The_Personal_Page()
    {
        using var server = new FamilyServer(); var expected = await server.AddMember(name: "Marc"); var other = await server.AddMember(name: "Caro");
        var (phone, _) = await server.Phone(expected); var (wrong, _) = await server.Phone(other); using var browser = server.Browser();
        var challenge = await server.Challenge(browser, expected); var code = challenge.GetProperty("code").GetString()!;
        Assert.Equal(HttpStatusCode.Forbidden, (await wrong.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(code))).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(code))).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await browser.PostAsync("/api/v1/auth/browser/consume", null)).StatusCode);
        Assert.Equal(expected.Id, (await browser.GetFromJsonAsync<Actor>("/api/v1/me"))!.MemberId);
    }
    [Fact] public async Task A_Claimed_Or_Consumed_Link_Cannot_Be_Reused_By_Another_Browser()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var (phone, _) = await server.Phone(member); var link = await server.LoginLink(member);
        using var first = server.Browser(); using var second = server.Browser(); var payload = new BrowserChallengeInput(FamilyServer.LinkSecret(link));
        var challenge = await (await first.PostAsJsonAsync("/api/v1/auth/browser/challenges", payload)).Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal(HttpStatusCode.Forbidden, (await second.PostAsJsonAsync("/api/v1/auth/browser/challenges", payload)).StatusCode);
        await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(challenge.GetProperty("code").GetString()!)); await first.PostAsync("/api/v1/auth/browser/consume", null);
        Assert.Equal(HttpStatusCode.NotFound, (await second.PostAsJsonAsync("/api/v1/auth/browser/challenges", payload)).StatusCode);
    }
    [Fact] public async Task Renewing_A_Code_Does_Not_Extend_The_Link_Lifetime()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var link = await server.LoginLink(member); using var browser = server.Browser();
        var payload = new BrowserChallengeInput(FamilyServer.LinkSecret(link));
        var first = await (await browser.PostAsJsonAsync("/api/v1/auth/browser/challenges", payload)).Content.ReadFromJsonAsync<JsonElement>();
        server.Clock.Now = server.Clock.Now.AddMinutes(4);
        var second = await (await browser.PostAsJsonAsync("/api/v1/auth/browser/challenges", payload)).Content.ReadFromJsonAsync<JsonElement>();
        Assert.NotEqual(first.GetProperty("code").GetString(), second.GetProperty("code").GetString());
        Assert.Equal(link.ExpiresAtUtc, second.GetProperty("expiresAtUtc").GetDateTimeOffset());
    }
    [Fact] public async Task Only_An_Admin_Browser_Can_Generate_A_Link()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true); var member = await server.AddMember();
        var adminUser = await server.ApprovedBrowserPrincipal(admin); var memberUser = await server.ApprovedBrowserPrincipal(member);
        await using var scope = server.Services.CreateAsyncScope(); var mediator = scope.ServiceProvider.GetRequiredService<IMediator>();
        Assert.Equal(403, (await mediator.CreateMemberLoginLink(memberUser, admin.Id)).Status);
        Assert.True((await mediator.CreateMemberLoginLink(adminUser, member.Id)).Success);
    }
}
