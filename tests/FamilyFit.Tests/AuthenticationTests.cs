using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using FamilyFit.Contracts;

namespace FamilyFit.Tests;

public sealed class AuthenticationTests
{
    [Fact] public async Task Invitation_Is_Personal_OneTime_And_Preview_Does_Not_Consume_It()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); using var client = server.Browser(); var invite = await server.Invite(member.Id);
        var preview = await client.PostAsJsonAsync("/api/v1/auth/invitations/preview", new ApproveRequest(invite.Code)); Assert.Equal(HttpStatusCode.OK, preview.StatusCode);
        var response = await client.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(invite.Code, "Android"));
        Assert.Equal(member.Id, (await response.Content.ReadFromJsonAsync<DeviceGrant>())!.Member.Id);
        Assert.Equal(HttpStatusCode.BadRequest, (await client.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(invite.Code, "iPhone"))).StatusCode);
    }
    [Fact] public async Task Invitation_Expires_And_New_Invitation_Invalidates_Older_One()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); using var client = server.Browser();
        var old = await server.Invite(member.Id); var current = await server.Invite(member.Id);
        Assert.Equal(HttpStatusCode.BadRequest, (await client.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(old.Code, "Android"))).StatusCode);
        server.Clock.Now = server.Clock.Now.AddMinutes(11);
        Assert.Equal(HttpStatusCode.BadRequest, (await client.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(current.Code, "Android"))).StatusCode);
    }
    [Fact] public async Task Browser_Login_Requires_Mobile_Approval_And_Original_Browser_Secret()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var (phone, _) = await server.Phone(member); using var browser = server.Browser(); using var attacker = server.Browser();
        var challenge = await server.Challenge(browser, member);
        Assert.False(challenge.TryGetProperty("browserSecret", out _));
        var pending = await (await browser.PostAsync("/api/v1/auth/browser/consume", null)).Content.ReadFromJsonAsync<JsonElement>(); Assert.Equal("pending", pending.GetProperty("state").GetString());
        Assert.Equal(HttpStatusCode.OK, (await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(challenge.GetProperty("code").GetString()!))).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await attacker.PostAsync("/api/v1/auth/browser/consume", null)).StatusCode);
        var consumed = await browser.PostAsync("/api/v1/auth/browser/consume", null); Assert.Equal(HttpStatusCode.OK, consumed.StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await browser.GetAsync("/api/v1/me")).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.PostAsync("/api/v1/auth/browser/consume", null)).StatusCode);
    }
    [Fact] public async Task Revocation_Invalidates_Both_Phone_And_Approved_Browser()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true, "Admin"); var user = await server.AddMember(); var (adminPhone, _) = await server.Phone(admin); var (phone, grant) = await server.Phone(user); using var browser = server.Browser();
        var challenge = await server.Challenge(browser, user);
        await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(challenge.GetProperty("code").GetString()!)); await browser.PostAsync("/api/v1/auth/browser/consume", null);
        Assert.Equal(HttpStatusCode.OK, (await adminPhone.PostAsync($"/api/v1/devices/{grant.DeviceId}/revoke", null)).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await phone.GetAsync("/api/v1/sync")).StatusCode);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.GetAsync("/api/v1/me")).StatusCode);
    }
    [Fact] public async Task Browser_Code_Expires_And_CrossOrigin_Creation_Is_Forbidden()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); using var browser = server.Browser();
        await server.Challenge(browser, member); server.Clock.Now = server.Clock.Now.AddMinutes(6);
        var result = await (await browser.PostAsync("/api/v1/auth/browser/consume", null)).Content.ReadFromJsonAsync<JsonElement>(); Assert.Equal("expired", result.GetProperty("state").GetString());
        using var stranger = server.Browser(); stranger.DefaultRequestHeaders.Add("Origin", "https://untrusted.example");
        Assert.Equal(HttpStatusCode.Forbidden, (await stranger.PostAsJsonAsync("/api/v1/auth/browser/challenges", new FamilyFit.Contracts.Models.Authentication.BrowserChallengeInput("invalid"))).StatusCode);
    }
    [Fact] public async Task Production_Has_No_Demo_Login_And_Anonymous_Requests_Cannot_Read_Data()
    {
        using var server = new FamilyServer(); using var browser = server.Browser();
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.GetAsync("/api/v1/family")).StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, (await browser.GetAsync("/demo")).StatusCode);
        Assert.Equal(HttpStatusCode.BadRequest, (await browser.PostAsJsonAsync("/api/v1/auth/enroll", new { code = (string?)null, deviceName = "Android" })).StatusCode);
    }
}
