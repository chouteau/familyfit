using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Devices;

public sealed record RevokeDeviceRequest(ClaimsPrincipal User, Guid Id) : IRequest<Outcome<bool>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => false; }
