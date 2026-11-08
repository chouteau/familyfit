using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Programs;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using FamilyFit.Core.Validators;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Programs;

public sealed class SaveProgramRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<SaveProgramRequest, WorkoutProgram>(auth, mediator)
{
    protected override async Task<Outcome<WorkoutProgram>> Execute(SaveProgramRequest request, Actor actor, CancellationToken ct)
    {
        if (request.Input == null) return Outcome<WorkoutProgram>.Fail("Programme absent.");
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var member = await db.Members.AsNoTracking().SingleOrDefaultAsync(x => x.Id == request.Input.MemberId, ct);
        if (member == null) return Outcome<WorkoutProgram>.Fail("Membre introuvable.", 404);
        var locations = await db.Locations.AsNoTracking().Where(x => x.MemberId == member.Id).ToListAsync(ct);
        var catalogue = await db.Exercises.AsNoTracking().ToListAsync(ct);
        var error = ProgramValidator.Validate(request.Input, member, locations, catalogue);
        if (error != null) return Outcome<WorkoutProgram>.Fail(error);
        var program = request.Id == null ? new WorkoutProgram { MemberId = member.Id, CreatedAtUtc = clock.GetUtcNow() } : await db.Programs.Include(x => x.Days).SingleOrDefaultAsync(x => x.Id == request.Id, ct);
        if (program == null) return Outcome<WorkoutProgram>.Fail("Programme introuvable.", 404);
        if (program.State != "draft" || program.MemberId != member.Id) return Outcome<WorkoutProgram>.Fail("Un programme publié est immuable. Crée un nouveau brouillon.", 409);
        program.Title = request.Input.Title.Trim(); program.WeekStart = request.Input.WeekStart;
        db.Days.RemoveRange(program.Days);
        program.Days = request.Input.Days.Select(x => new ProgramDay { ProgramId = program.Id, Date = x.Date, Title = x.Title.Trim(), LocationId = x.IsRest ? null : x.LocationId, DurationMinutes = x.IsRest ? 0 : x.DurationMinutes, IsRest = x.IsRest, Exercises = x.Exercises }).ToList();
        if (request.Id == null) db.Programs.Add(program);
        await db.SaveChangesAsync(ct);
        await tx.CommitAsync(ct);
        return Outcome<WorkoutProgram>.Ok(program);
    }
}
