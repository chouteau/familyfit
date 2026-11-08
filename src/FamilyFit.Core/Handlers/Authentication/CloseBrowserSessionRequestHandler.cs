using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;
namespace FamilyFit.Core.Handlers.Authentication;
public sealed class CloseBrowserSessionRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock) : AuthorizedHandler<CloseBrowserSessionRequest, bool>(auth, mediator)
{
    protected override async Task<Outcome<bool>> Execute(CloseBrowserSessionRequest request, Actor actor, CancellationToken ct)
    {
        if (actor.SessionId == null) return Outcome<bool>.Fail("Cette action concerne une session du navigateur.", 400);
        await using var db = await factory.CreateDbContextAsync(ct);
        await db.BrowserSessions.Where(x => x.Id == actor.SessionId).ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAtUtc, clock.GetUtcNow()), ct);
        return Outcome<bool>.Ok(true);
    }
}
