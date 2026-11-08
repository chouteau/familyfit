using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Equipment;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Equipment;

public sealed class GetEquipmentCatalogueRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<GetEquipmentCatalogueRequest, List<EquipmentDefinition>>(auth, mediator)
{
    protected override async Task<Outcome<List<EquipmentDefinition>>> Execute(GetEquipmentCatalogueRequest request, Actor actor, CancellationToken ct)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        return Outcome<List<EquipmentDefinition>>.Ok(await db.Equipment.AsNoTracking().OrderBy(x => x.Name).ThenBy(x => x.Id).ToListAsync(ct));
    }
}
