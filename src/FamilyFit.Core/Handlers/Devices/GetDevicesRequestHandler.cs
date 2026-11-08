using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Devices;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Devices;

public sealed class GetDevicesRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<GetDevicesRequest, List<DeviceView>>(auth, mediator)
{
    protected override async Task<Outcome<List<DeviceView>>> Execute(GetDevicesRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        var devices = await db.Devices.AsNoTracking().Where(x => actor.IsAdmin || x.MemberId == actor.MemberId).OrderByDescending(x => x.CreatedAtUtc).Select(x => new DeviceView(x.Id, x.MemberId, x.Name, x.CreatedAtUtc, x.RevokedAtUtc)).ToListAsync(ct);
        return Outcome<List<DeviceView>>.Ok(devices);
    }
}
