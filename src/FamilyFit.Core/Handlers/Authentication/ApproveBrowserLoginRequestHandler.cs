using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class ApproveBrowserLoginRequestHandler(AuthService auth, IMediator mediator)
    : AuthorizedHandler<ApproveBrowserLoginRequest, bool>(auth, mediator)
{
    protected override async Task<Outcome<bool>> Execute(ApproveBrowserLoginRequest request, Actor actor, CancellationToken ct)
    {
        await Auth.ApproveAsync(actor, request.Code, ct);
        return Outcome<bool>.Ok(true);
    }
}
