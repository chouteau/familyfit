using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Workouts;

public sealed record GetSyncRequest(ClaimsPrincipal User) : IRequest<Outcome<SyncSnapshot>>, IAuthorizedRequest, IHumanOnlyRequest { public bool AdminOnly => false; }
