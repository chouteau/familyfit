using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Locations;

public static class LocationsExtensions
{
    public static Task<Outcome<Location>> SaveLocation(this IMediator mediator, ClaimsPrincipal User, Guid? Id, LocationInput Input, CancellationToken cancellationToken = default) => mediator.Send(new SaveLocationRequest(User, Id, Input), cancellationToken);
}
