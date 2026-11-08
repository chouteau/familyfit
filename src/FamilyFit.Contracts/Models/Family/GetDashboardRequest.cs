using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Family;

public sealed record GetDashboardRequest(ClaimsPrincipal User) : IRequest<Outcome<DashboardData>>, IAuthorizedRequest { public bool AdminOnly => false; }
