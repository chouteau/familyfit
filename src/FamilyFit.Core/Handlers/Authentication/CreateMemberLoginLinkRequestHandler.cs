using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class CreateMemberLoginLinkRequestHandler(AuthService auth, IMediator mediator) : AuthorizedHandler<CreateMemberLoginLinkRequest, MemberLoginLinkCreated>(auth, mediator)
{
    protected override async Task<Outcome<MemberLoginLinkCreated>> Execute(CreateMemberLoginLinkRequest request, Actor actor, CancellationToken ct)
    {
        AgentAuthorization.RequireBrowser(request.User);
        return Outcome<MemberLoginLinkCreated>.Ok(await Auth.CreateMemberLoginLinkAsync(request.MemberId, ct));
    }
}
