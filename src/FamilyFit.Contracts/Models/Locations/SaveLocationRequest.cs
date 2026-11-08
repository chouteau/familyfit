using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Locations;

public sealed record SaveLocationRequest(ClaimsPrincipal User, Guid? Id, LocationInput Input) : IRequest<Outcome<Location>>, IAuthorizedRequest, IChangesFamily { public bool AdminOnly => false; }
