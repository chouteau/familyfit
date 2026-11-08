using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Programs;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Programs;

public sealed class GenerateProgramRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, AiProgramService ai)
    : AuthorizedHandler<GenerateProgramRequest, WorkoutProgram>(auth, mediator)
{
    protected override async Task<Outcome<WorkoutProgram>> Execute(GenerateProgramRequest request, Actor actor, CancellationToken ct)
    {
        if (request.Input == null || request.Input.WeekStart.DayOfWeek != DayOfWeek.Monday) return Outcome<WorkoutProgram>.Fail("Choisis une semaine commençant un lundi.");
        await using var db = await factory.CreateDbContextAsync(ct);
        var member = await db.Members.AsNoTracking().SingleOrDefaultAsync(x => x.Id == request.Input.MemberId, ct);
        var location = await db.Locations.AsNoTracking().SingleOrDefaultAsync(x => x.Id == request.Input.LocationId && x.MemberId == request.Input.MemberId, ct);
        if (member == null || location == null) return Outcome<WorkoutProgram>.Fail("Choisis un membre et l’un de ses lieux d’entraînement.");
        var proposal = await ai.Generate(member, location, request.Input.WeekStart, await db.Exercises.AsNoTracking().ToListAsync(ct), ct);
        if (!proposal.Success) return Outcome<WorkoutProgram>.Fail(proposal.Error!, proposal.Status);
        return await Dispatcher.SaveProgram(request.User, null, proposal.Value!, ct);
    }
}
