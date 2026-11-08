using System.Net;
using System.Net.Http.Json;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace FamilyFit.Tests;

public sealed class WorkoutTests
{
    [Fact] public async Task Concurrent_Edit_And_Publication_Preserve_The_Published_Day_Identifiers()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, draft) = await server.Plan(phone, member, false);
        var input = new ProgramInput(member.Id, "Nouvelle version", draft.WeekStart, draft.Days.Select(x => new ProgramDayInput(x.Date, x.Title, x.LocationId, x.DurationMinutes, x.IsRest, x.Exercises)).ToList());
        var edit = phone.PutAsJsonAsync($"/api/v1/programs/{draft.Id}", input);
        var publish = phone.PostAsync($"/api/v1/programs/{draft.Id}/publish", null);
        await Task.WhenAll(edit, publish);
        var publication = await publish; var saved = await edit;
        Assert.Equal(HttpStatusCode.OK, publication.StatusCode);
        Assert.Contains(saved.StatusCode, new[] { HttpStatusCode.OK, HttpStatusCode.Conflict });
        var published = (await publication.Content.ReadFromJsonAsync<WorkoutProgram>())!;
        var downloaded = (await phone.GetFromJsonAsync<SyncSnapshot>("/api/v1/sync"))!;
        Assert.Equal(published.Days.Select(x => x.Id).Order(), downloaded.Days.Select(x => x.Id).Order());
        Assert.Equal(HttpStatusCode.Conflict, (await phone.PutAsJsonAsync($"/api/v1/programs/{draft.Id}", input)).StatusCode);
    }
    [Fact] public async Task Null_Performance_Is_A_Validation_Error()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, program) = await server.Plan(phone, member);
        var input = server.Submission(program) with { Exercises = [null!] };
        Assert.Equal(HttpStatusCode.BadRequest, (await phone.PostAsJsonAsync("/api/v1/workouts", input)).StatusCode);
    }
    [Theory] [InlineData(3, "completed")] [InlineData(1, "partial")] [InlineData(0, "abandoned")]
    public async Task Completion_Is_Computed_From_Actual_Performance(int sets, string status)
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, program) = await server.Plan(phone, member);
        var response = await phone.PostAsJsonAsync("/api/v1/workouts", server.Submission(program, sets)); Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(status, (await response.Content.ReadFromJsonAsync<WorkoutResultView>())!.Status);
    }
    [Fact] public async Task Concurrent_Retries_Record_Exactly_One_Workout_And_Reject_Changed_Payload()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, program) = await server.Plan(phone, member); var input = server.Submission(program);
        var responses = await Task.WhenAll(phone.PostAsJsonAsync("/api/v1/workouts", input), phone.PostAsJsonAsync("/api/v1/workouts", input));
        Assert.All(responses, response => Assert.Equal(HttpStatusCode.OK, response.StatusCode));
        var sync = (await phone.GetFromJsonAsync<SyncSnapshot>("/api/v1/sync"))!; Assert.Single(sync.Results);
        var changed = input with { Exercises = [input.Exercises[0] with { CompletedSets = 1 }] };
        Assert.Equal(HttpStatusCode.Conflict, (await phone.PostAsJsonAsync("/api/v1/workouts", changed)).StatusCode);
    }
    [Fact] public async Task Drafts_Are_Not_Downloaded_And_Published_Programs_Are_Immutable()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, program) = await server.Plan(phone, member, false);
        Assert.Empty((await phone.GetFromJsonAsync<SyncSnapshot>("/api/v1/sync"))!.Days);
        Assert.Equal(HttpStatusCode.Forbidden, (await phone.PostAsJsonAsync("/api/v1/workouts", server.Submission(program))).StatusCode);
        await phone.PostAsync($"/api/v1/programs/{program.Id}/publish", null);
        var input = new ProgramInput(member.Id, "Changed", program.WeekStart, program.Days.Select(x => new ProgramDayInput(x.Date, x.Title, x.LocationId, x.DurationMinutes, x.IsRest, x.Exercises)).ToList());
        Assert.Equal(HttpStatusCode.Conflict, (await phone.PutAsJsonAsync($"/api/v1/programs/{program.Id}", input)).StatusCode);
    }
    [Fact] public async Task A_Member_Cannot_Edit_Another_Profile_Or_Publish_A_Program()
    {
        using var server = new FamilyServer(); var admin = await server.AddMember(true, "Admin"); var user = await server.AddMember(); var (adminPhone, _) = await server.Phone(admin); var (phone, _) = await server.Phone(user); var (_, draft) = await server.Plan(adminPhone, user, false);
        Assert.Equal(HttpStatusCode.Forbidden, (await phone.PostAsync($"/api/v1/programs/{draft.Id}/publish", null)).StatusCode);
        var input = new MemberInput("Modifié", 30, 70, "Forme", "Débutant", 30, "1,3,5", "", admin.Revision);
        Assert.Equal(HttpStatusCode.Forbidden, (await phone.PutAsJsonAsync($"/api/v1/members/{admin.Id}", input)).StatusCode);
        Assert.Equal(HttpStatusCode.OK, (await phone.GetAsync("/api/v1/family")).StatusCode);
    }
    [Fact] public async Task Incompatible_Equipment_Is_Rejected_And_Stale_Profile_Does_Not_Overwrite()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (location, program) = await server.Plan(phone, member, false);
        var input = new ProgramInput(member.Id, "Incompatible", program.WeekStart, program.Days.Select(x => new ProgramDayInput(x.Date, x.Title, location.Id, 30, false, [new Prescription("lat_pulldown", 3, 12)])).ToList());
        Assert.Equal(HttpStatusCode.BadRequest, (await phone.PostAsJsonAsync("/api/v1/programs", input)).StatusCode);
        var profile = new MemberInput("Camille", 30, 71, "Forme", "Débutant", 30, "1,3,5", "", member.Revision);
        Assert.Equal(HttpStatusCode.OK, (await phone.PutAsJsonAsync($"/api/v1/members/{member.Id}", profile)).StatusCode);
        Assert.Equal(HttpStatusCode.Conflict, (await phone.PutAsJsonAsync($"/api/v1/members/{member.Id}", profile)).StatusCode);
    }
    [Fact] public async Task Archived_Program_Accepts_A_Previously_Downloaded_Offline_Result()
    {
        using var server = new FamilyServer(); var member = await server.AddMember(true); var (phone, _) = await server.Phone(member); var (_, old) = await server.Plan(phone, member); await server.Plan(phone, member);
        Assert.Equal(HttpStatusCode.OK, (await phone.PostAsJsonAsync("/api/v1/workouts", server.Submission(old))).StatusCode);
        var family = (await phone.GetFromJsonAsync<DashboardData>("/api/v1/family"))!;
        Assert.Equal("archived", family.Programs.Single(x => x.Id == old.Id).State);
        Assert.Single(family.Programs, x => x.State == "published");
    }
    [Theory] [InlineData("2026-03-29T22:30:00Z", "2026-03-30")] [InlineData("2026-10-25T23:30:00Z", "2026-10-26")]
    public void Calendar_Uses_Paris_Dates_Across_DST(string instant, string expected)
    {
        var clock = new TestClock { Now = DateTimeOffset.Parse(instant) };
        Assert.Equal(DateOnly.Parse(expected), FamilyCalendar.Today(clock));
    }
}
