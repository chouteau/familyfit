using ChannelMediator;
using System.Security.Claims;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts.Models.Authentication;

public sealed record GetActorRequest(ClaimsPrincipal User, bool AdminOnly = false) : IRequest<Outcome<Actor>>, IAuthorizedRequest;
