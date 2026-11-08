using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Programs;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using FamilyFit.Core.Validators;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Programs;

public sealed class PublishProgramRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<PublishProgramRequest, WorkoutProgram>(auth, mediator)
{
    protected override async Task<Outcome<WorkoutProgram>> Execute(PublishProgramRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var program = await db.Programs.Include(x => x.Days).SingleOrDefaultAsync(x => x.Id == request.Id, ct);
        if (program == null) return Outcome<WorkoutProgram>.Fail("Programme introuvable.", 404);
        if (program.State != "draft") return Outcome<WorkoutProgram>.Fail("Seul un brouillon peut être publié.", 409);
        var member = await db.Members.AsNoTracking().SingleAsync(x => x.Id == program.MemberId, ct);
        var input = new ProgramInput(member.Id, program.Title, program.WeekStart, program.Days.Select(x => new ProgramDayInput(x.Date, x.Title, x.LocationId, x.DurationMinutes, x.IsRest, x.Exercises)).ToList());
        var error = ProgramValidator.Validate(input, member, await db.Locations.AsNoTracking().Where(x => x.MemberId == member.Id).ToListAsync(ct), await db.Exercises.AsNoTracking().ToListAsync(ct));
        if (error != null) return Outcome<WorkoutProgram>.Fail(error);
        var previous = await db.Programs.Where(x => x.MemberId == member.Id && x.WeekStart == program.WeekStart && x.State == "published").ToListAsync(ct);
        foreach (var old in previous) old.State = "archived";
        program.Version = (await db.Programs.Where(x => x.Id != program.Id && x.MemberId == member.Id && x.WeekStart == program.WeekStart).MaxAsync(x => (int?)x.Version, ct) ?? 0) + 1;
        program.State = "published"; program.ApprovedBy = actor.MemberId; program.PublishedAtUtc = clock.GetUtcNow();
        await db.SaveChangesAsync(ct); await tx.CommitAsync(ct);
        return Outcome<WorkoutProgram>.Ok(program);
    }
}
