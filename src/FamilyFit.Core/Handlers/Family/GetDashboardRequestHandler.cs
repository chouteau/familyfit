using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Family;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Family;

public sealed class GetDashboardRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<GetDashboardRequest, DashboardData>(auth, mediator)
{
    protected override async Task<Outcome<DashboardData>> Execute(GetDashboardRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var data = await ReadModels.Dashboard(db, actor.IsAdmin, ct);
        await tx.CommitAsync(ct);
        return Outcome<DashboardData>.Ok(data);
    }
}
