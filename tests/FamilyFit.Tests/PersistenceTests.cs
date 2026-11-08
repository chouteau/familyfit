using System.Net;
using System.Net.Http.Json;
using System.Text.Json;
using FamilyFit.Contracts;
using FamilyFit.Core.Data;
using FamilyFit.Web.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace FamilyFit.Tests;

public sealed class PersistenceTests
{
    [Fact] public async Task Online_Backup_Is_Readable_And_Contains_Committed_Family_Data()
    {
        using var server = new FamilyServer(); await server.AddMember(true);
        await using var scope = server.Services.CreateAsyncScope();
        Assert.True(await OperatorCommands.TryRunAsync(["--backup", "--backup-name", "verification.db"], scope.ServiceProvider, scope.ServiceProvider.GetRequiredService<IWebHostEnvironment>()));
        await using var backup = new SqliteConnection($"Data Source={Path.Combine(server.DirectoryPath, "backups", "verification.db")};Mode=ReadOnly"); await backup.OpenAsync();
        await using var command = backup.CreateCommand(); command.CommandText = "PRAGMA integrity_check"; Assert.Equal("ok", await command.ExecuteScalarAsync());
        command.CommandText = "SELECT COUNT(*) FROM Members"; Assert.Equal(1L, await command.ExecuteScalarAsync());
    }
    [Fact] public async Task A_Workout_Can_Resume_After_An_Overnight_Pause()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, program) = await server.Plan(phone, member);
        var submission = server.Submission(program) with { StartedAtUtc = server.Clock.Now.AddHours(-36) };
        Assert.Equal(HttpStatusCode.OK, (await phone.PostAsJsonAsync("/api/v1/workouts", submission)).StatusCode);
    }
    [Fact] public async Task Browser_Session_Expires_After_Twelve_Hours_Without_Expiring_The_Phone()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var (phone, _) = await server.Phone(member); using var browser = server.Browser();
        var challenge = await server.Challenge(browser, member);
        await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(challenge.GetProperty("code").GetString()!)); await browser.PostAsync("/api/v1/auth/browser/consume", null);
        server.Clock.Now = server.Clock.Now.AddHours(13);
        Assert.Equal(HttpStatusCode.Unauthorized, (await browser.GetAsync("/api/v1/me")).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await phone.GetAsync("/api/v1/sync")).StatusCode);
    }
    [Fact] public async Task Invitations_And_Login_Codes_Cannot_Be_Used_For_The_Other_Purpose()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(); var (phone, _) = await server.Phone(member); using var browser = server.Browser();
        var invitation = await server.Invite(member.Id);
        Assert.Equal(HttpStatusCode.BadRequest, (await phone.PostAsJsonAsync("/api/v1/auth/browser/approve", new ApproveRequest(invitation.Code))).StatusCode);
        var challenge = await server.Challenge(browser, member);
        Assert.Equal(HttpStatusCode.BadRequest, (await browser.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(challenge.GetProperty("code").GetString()!, "Android"))).StatusCode);
    }
}
