using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Devices;

public sealed record RevokeDeviceRequest(ClaimsPrincipal User, Guid Id) : IRequest<Outcome<bool>>, IAuthorizedRequest, IHumanOnlyRequest, IChangesFamily { public bool AdminOnly => false; }
