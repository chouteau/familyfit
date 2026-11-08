using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Services;

namespace FamilyFit.Core.Handlers.Authentication;

public sealed class EnrollDeviceRequestHandler(AuthService auth) : IRequestHandler<EnrollDeviceRequest, Outcome<DeviceGrant>>
{
    public async Task<Outcome<DeviceGrant>> Handle(EnrollDeviceRequest request, CancellationToken ct)
    {
        try { return Outcome<DeviceGrant>.Ok(await auth.EnrollAsync(request.Input, ct)); }
        catch (DomainException ex) { return Outcome<DeviceGrant>.Fail(ex.Message, ex.Status); }
    }
}
