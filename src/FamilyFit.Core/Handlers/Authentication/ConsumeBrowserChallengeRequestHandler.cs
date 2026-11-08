using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class ConsumeBrowserChallengeRequestHandler(AuthService auth, IDbContextFactory<FamilyDb> factory) : IRequestHandler<ConsumeBrowserChallengeRequest, Outcome<BrowserLoginResult>>
{
    public async Task<Outcome<BrowserLoginResult>> Handle(ConsumeBrowserChallengeRequest request, CancellationToken ct)
    {
        try
        {
            var (state, session) = await auth.ConsumeAsync(request.Id, request.BrowserSecret, ct);
            await using var db = await factory.CreateDbContextAsync(ct);
            var member = session == null ? null : await db.Members.AsNoTracking().SingleAsync(x => x.Id == session.MemberId, ct);
            return Outcome<BrowserLoginResult>.Ok(new(state, session, member));
        }
        catch (DomainException ex) { return Outcome<BrowserLoginResult>.Fail(ex.Message, ex.Status); }
    }
}
