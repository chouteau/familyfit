using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class CreateInvitationRequestHandler(AuthService auth, IMediator mediator)
    : AuthorizedHandler<CreateInvitationRequest, InvitationGrant>(auth, mediator)
{
    protected override async Task<Outcome<InvitationGrant>> Execute(CreateInvitationRequest request, Actor actor, CancellationToken ct) => Outcome<InvitationGrant>.Ok(await Auth.CreateInvitationAsync(request.MemberId, ct));
}
