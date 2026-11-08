using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class PreviewMemberLoginLinkRequestHandler(AuthService auth) : IRequestHandler<PreviewMemberLoginLinkRequest, Outcome<MemberLoginPreview?>>
{
    public async Task<Outcome<MemberLoginPreview?>> Handle(PreviewMemberLoginLinkRequest request, CancellationToken ct)
        => Outcome<MemberLoginPreview?>.Ok(await auth.PreviewMemberLoginLinkAsync(request.LinkSecret, request.BrowserSecret, ct));
}
