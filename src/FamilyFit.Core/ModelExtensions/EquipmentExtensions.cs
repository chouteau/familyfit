using System.Security.Claims;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Equipment;

public static class EquipmentExtensions
{
    public static Task<Outcome<List<EquipmentDefinition>>> GetEquipmentCatalogue(this IMediator mediator, ClaimsPrincipal user, CancellationToken cancellationToken = default)
        => mediator.Send(new GetEquipmentCatalogueRequest(user), cancellationToken);
    public static Task<Outcome<EquipmentDefinition>> SaveEquipment(this IMediator mediator, ClaimsPrincipal user, EquipmentInput input, CancellationToken cancellationToken = default)
        => mediator.Send(new SaveEquipmentRequest(user, input), cancellationToken);
    public static Task<Outcome<EquipmentDefinition>> SaveEquipmentImage(this IMediator mediator, ClaimsPrincipal user, string equipmentId, EquipmentImageInput input, CancellationToken cancellationToken = default)
        => mediator.Send(new SaveEquipmentImageRequest(user, equipmentId, input), cancellationToken);
    public static Task<Outcome<EquipmentImage>> GetEquipmentImage(this IMediator mediator, ClaimsPrincipal user, string equipmentId, CancellationToken cancellationToken = default)
        => mediator.Send(new GetEquipmentImageRequest(user, equipmentId), cancellationToken);
}
