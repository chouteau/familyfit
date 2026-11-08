using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Devices;

public static class DevicesExtensions
{
    public static Task<Outcome<List<DeviceView>>> GetDevices(this IMediator mediator, ClaimsPrincipal User, CancellationToken cancellationToken = default) => mediator.Send(new GetDevicesRequest(User), cancellationToken);
    public static Task<Outcome<bool>> RevokeDevice(this IMediator mediator, ClaimsPrincipal User, Guid Id, CancellationToken cancellationToken = default) => mediator.Send(new RevokeDeviceRequest(User, Id), cancellationToken);
}
