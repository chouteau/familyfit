using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Workouts;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Workouts;

public sealed class GetSyncRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<GetSyncRequest, SyncSnapshot>(auth, mediator)
{
    protected override async Task<Outcome<SyncSnapshot>> Execute(GetSyncRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var member = await db.Members.SingleAsync(x => x.Id == actor.MemberId, ct);
        member.LastSyncUtc = clock.GetUtcNow();
        await db.SaveChangesAsync(ct);
        var data = await ReadModels.Dashboard(db, false, ct);
        var days = data.Programs.Where(x => x.MemberId == actor.MemberId && x.State == "published").SelectMany(x => x.Days).OrderBy(x => x.Date).Select(x => ReadModels.Day(x, data.Exercises, data.Locations)).ToList();
        var result = new SyncSnapshot(clock.GetUtcNow(), member, data.Members, data.Locations, data.Exercises, days, data.Results.Where(x => x.MemberId == actor.MemberId).ToList(), data.Weights.Where(x => x.MemberId == actor.MemberId).ToList());
        await tx.CommitAsync(ct);
        return Outcome<SyncSnapshot>.Ok(result);
    }
}
