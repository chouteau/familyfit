using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Devices;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Devices;

public sealed class RevokeDeviceRequestHandler(AuthService auth, IMediator mediator)
    : AuthorizedHandler<RevokeDeviceRequest, bool>(auth, mediator)
{
    protected override async Task<Outcome<bool>> Execute(RevokeDeviceRequest request, Actor actor, CancellationToken ct)
    {
        await Auth.RevokeAsync(actor, request.Id, ct);
        return Outcome<bool>.Ok(true);
    }
}
