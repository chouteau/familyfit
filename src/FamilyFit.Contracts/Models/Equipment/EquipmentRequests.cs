using System.Security.Claims;
using ChannelMediator;

namespace FamilyFit.Contracts.Models.Equipment;

public sealed record GetEquipmentCatalogueRequest(ClaimsPrincipal User)
    : IRequest<Outcome<List<EquipmentDefinition>>>, IAuthorizedRequest { public bool AdminOnly => false; }
public sealed record SaveEquipmentRequest(ClaimsPrincipal User, EquipmentInput Input)
    : IRequest<Outcome<EquipmentDefinition>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => true; }
public sealed record SaveEquipmentImageRequest(ClaimsPrincipal User, string EquipmentId, EquipmentImageInput Input)
    : IRequest<Outcome<EquipmentDefinition>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => true; }
public sealed record GetEquipmentImageRequest(ClaimsPrincipal User, string EquipmentId)
    : IRequest<Outcome<EquipmentImage>>, IAuthorizedRequest { public bool AdminOnly => false; }
