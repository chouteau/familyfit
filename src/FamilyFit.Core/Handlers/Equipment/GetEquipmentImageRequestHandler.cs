using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Equipment;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Equipment;

public sealed class GetEquipmentImageRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<GetEquipmentImageRequest, EquipmentImage>(auth, mediator)
{
    protected override async Task<Outcome<EquipmentImage>> Execute(GetEquipmentImageRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        var image = await db.EquipmentImages.AsNoTracking().SingleOrDefaultAsync(x => x.EquipmentId == request.EquipmentId, ct);
        return image == null ? Outcome<EquipmentImage>.Fail("Photo introuvable.", 404) : Outcome<EquipmentImage>.Ok(image);
    }
}
