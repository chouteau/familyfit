using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;
namespace FamilyFit.Core.Handlers.Authentication;
public sealed class PreviewInvitationRequestHandler(IDbContextFactory<FamilyDb> factory, TimeProvider clock) : IRequestHandler<PreviewInvitationRequest, Outcome<InvitationPreview>>
{
    public async Task<Outcome<InvitationPreview>> Handle(PreviewInvitationRequest request, CancellationToken ct)
    {
        try
        {
            var hash = AuthService.Hash(AuthService.NormalizeCode(request.Code));
            await using var db = await factory.CreateDbContextAsync(ct);
            var invitation = await db.Invitations.AsNoTracking().SingleOrDefaultAsync(x => x.CodeHash == hash && x.UsedAtUtc == null && x.ExpiresAtUtc > clock.GetUtcNow(), ct);
            if (invitation == null) return Outcome<InvitationPreview>.Fail("Invitation expirée ou déjà utilisée.");
            var nickname = await db.Members.Where(x => x.Id == invitation.MemberId).Select(x => x.Nickname).SingleAsync(ct);
            return Outcome<InvitationPreview>.Ok(new(invitation.MemberId, nickname, invitation.ExpiresAtUtc));
        }
        catch (DomainException ex) { return Outcome<InvitationPreview>.Fail(ex.Message, ex.Status); }
    }
}
