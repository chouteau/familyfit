using System.Net.Http.Headers;
using System.Net.Http.Json;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.AspNetCore.DataProtection;

namespace FamilyFit.Tests;

public sealed class TestClock : TimeProvider
{
    public DateTimeOffset Now { get; set; } = DateTimeOffset.UtcNow;
    public override DateTimeOffset GetUtcNow() => Now;
}
public sealed class FamilyServer : WebApplicationFactory<Program>
{
    public string DirectoryPath { get; } = Path.Combine(Path.GetTempPath(), "FamilyFit.Tests", Guid.NewGuid().ToString("N"));
    public TestClock Clock { get; } = new();
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.ConfigureAppConfiguration((_, config) => config.AddInMemoryCollection(new Dictionary<string, string?> { ["FamilyFit:DataDirectory"] = DirectoryPath, ["FamilyFit:Demo"] = "false", ["OpenAI:ApiKey"] = "" }));
        builder.ConfigureServices(services => { services.RemoveAll<TimeProvider>(); services.AddSingleton<TimeProvider>(Clock); services.AddDataProtection().PersistKeysToFileSystem(new DirectoryInfo(Path.Combine(DirectoryPath, "keys"))); });
    }
    public HttpClient Browser() { var client = CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false, BaseAddress = new Uri("https://localhost") }); client.DefaultRequestHeaders.Add("X-FamilyFit-Request", "1"); return client; }
    public async Task<Member> AddMember(bool admin = false, string name = "Camille")
    {
        await using var scope = Services.CreateAsyncScope();
        await using var db = await scope.ServiceProvider.GetRequiredService<IDbContextFactory<FamilyDb>>().CreateDbContextAsync();
        Assert.StartsWith(DirectoryPath, new SqliteConnectionStringBuilder(db.Database.GetConnectionString()).DataSource);
        var member = new Member { Nickname = name, Age = 30, WeightKg = 70, IsAdmin = admin };
        db.Members.Add(member); await db.SaveChangesAsync(); return member;
    }
    public async Task<InvitationGrant> Invite(Guid memberId)
    {
        await using var scope = Services.CreateAsyncScope();
        return await scope.ServiceProvider.GetRequiredService<AuthService>().CreateInvitationAsync(memberId);
    }
    public async Task<(HttpClient Client, DeviceGrant Grant)> Phone(Member member)
    {
        var invitation = await Invite(member.Id); var client = Browser();
        var response = await client.PostAsJsonAsync("/api/v1/auth/enroll", new EnrollRequest(invitation.Code, "Téléphone de test"));
        Assert.True(response.IsSuccessStatusCode, await response.Content.ReadAsStringAsync());
        var grant = (await response.Content.ReadFromJsonAsync<DeviceGrant>())!;
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", grant.Token);
        return (client, grant);
    }
    public async Task<(Location Location, WorkoutProgram Program)> Plan(HttpClient admin, Member member, bool publish = true)
    {
        var locationResponse = await admin.PostAsJsonAsync("/api/v1/locations", new LocationInput(member.Id, "Maison", ["BODYWEIGHT"]));
        Assert.True(locationResponse.IsSuccessStatusCode, await locationResponse.Content.ReadAsStringAsync());
        var location = (await locationResponse.Content.ReadFromJsonAsync<Location>())!;
        var monday = FamilyCalendar.Monday(FamilyCalendar.Today(Clock));
        var input = new ProgramInput(member.Id, "Semaine de test", monday, Enumerable.Range(0, 7).Select(i => new ProgramDayInput(monday.AddDays(i), "Gainage", location.Id, 20, false, [new Prescription("bodyweight_plank", 3, 40, 20)])).ToList());
        // Discover the real imported catalogue rather than assuming the Android identifier spelling.
        var family = (await admin.GetFromJsonAsync<DashboardData>("/api/v1/family"))!;
        var exercise = family.Exercises.First(x => x.EquipmentId == "BODYWEIGHT");
        input = input with { Days = input.Days.Select(x => x with { Exercises = [new Prescription(exercise.Id, 3, 12, 20)] }).ToList() };
        var response = await admin.PostAsJsonAsync("/api/v1/programs", input);
        Assert.True(response.IsSuccessStatusCode, await response.Content.ReadAsStringAsync());
        var program = (await response.Content.ReadFromJsonAsync<WorkoutProgram>())!;
        if (publish) { response = await admin.PostAsync($"/api/v1/programs/{program.Id}/publish", null); Assert.True(response.IsSuccessStatusCode, await response.Content.ReadAsStringAsync()); program = (await response.Content.ReadFromJsonAsync<WorkoutProgram>())!; }
        return (location, program);
    }
    public WorkoutSubmission Submission(WorkoutProgram program, int completedSets = 3) => new(Guid.NewGuid(), program.Days[0].Id, Clock.Now.AddMinutes(-10), Clock.Now, [new ExercisePerformance(program.Days[0].Exercises[0].ExerciseId, completedSets)]);
    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        if (!disposing) return;
        SqliteConnection.ClearAllPools();
        var root = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "FamilyFit.Tests")) + Path.DirectorySeparatorChar;
        if (Path.GetFullPath(DirectoryPath).StartsWith(root, StringComparison.OrdinalIgnoreCase) && Directory.Exists(DirectoryPath)) Directory.Delete(DirectoryPath, true);
    }
}
