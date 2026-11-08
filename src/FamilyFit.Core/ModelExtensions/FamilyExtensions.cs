using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Family;

public static class FamilyExtensions
{
    public static Task<Outcome<DashboardData>> GetDashboard(this IMediator mediator, ClaimsPrincipal User, CancellationToken cancellationToken = default) => mediator.Send(new GetDashboardRequest(User), cancellationToken);
}
